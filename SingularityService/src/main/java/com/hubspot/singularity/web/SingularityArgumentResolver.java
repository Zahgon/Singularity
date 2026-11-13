package com.hubspot.singularity.web;

import com.hubspot.singularity.web.ParamConverters.Source;
import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

/**
 * Reads the parameters of a resource method out of the request: a value of the path, a value of the query or
 * a value of a header.
 *
 * A parameter that holds one value is read from the first value the request carries under that name, even
 * when the request carries it more than once; a parameter that holds a collection is read from every value,
 * in the order the request carries them. A parameter the request does not carry at all is read as the value
 * it names as its default, or as nothing; a parameter the request carries empty is read from the empty text.
 */
public class SingularityArgumentResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return (
      parameter.hasParameterAnnotation(QueryParam.class) ||
      parameter.hasParameterAnnotation(PathParam.class) ||
      parameter.hasParameterAnnotation(HeaderParam.class)
    );
  }

  @Override
  public Object resolveArgument(
    MethodParameter parameter,
    ModelAndViewContainer mavContainer,
    NativeWebRequest webRequest,
    WebDataBinderFactory binderFactory
  )
    throws Exception {
    final HttpServletRequest request = webRequest.getNativeRequest(
      HttpServletRequest.class
    );
    final PathParam path = parameter.getParameterAnnotation(PathParam.class);
    final QueryParam query = parameter.getParameterAnnotation(QueryParam.class);
    final HeaderParam header = parameter.getParameterAnnotation(HeaderParam.class);
    final DefaultValue fallback = parameter.getParameterAnnotation(DefaultValue.class);

    final String name = path != null
      ? path.value()
      : query != null ? query.value() : header.value();
    final Source source = path != null
      ? Source.PATH
      : query != null ? Source.QUERY : Source.HEADER;
    final List<String> values = values(request, name, source);
    return read(parameter, name, source, values, fallback == null ? null : fallback.value());
  }

  @SuppressWarnings("unchecked")
  private List<String> values(HttpServletRequest request, String name, Source source) {
    final List<String> values = new ArrayList<>();
    switch (source) {
      case PATH:
        final Object variables = request.getAttribute(
          HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
        );
        if (variables instanceof Map) {
          final Object value = ((Map<String, String>) variables).get(name);
          if (value != null) {
            values.add(decoded((String) value));
          }
        }
        return values;
      case QUERY:
        return QueryParameters.all(request, name);
      default:
        final Enumeration<String> headers = request.getHeaders(name);
        while (headers != null && headers.hasMoreElements()) {
          values.add(headers.nextElement());
        }
        return values;
    }
  }

  /** A value of the path with its escapes read out, which is how the service has always read one. */
  private static String decoded(String value) {
    if (value.indexOf('%') < 0) {
      return value;
    }
    try {
      return java.net.URLDecoder.decode(
        value.replace("+", "%2B"),
        java.nio.charset.StandardCharsets.UTF_8.name()
      );
    } catch (java.io.UnsupportedEncodingException | IllegalArgumentException e) {
      return value;
    }
  }

  private Object read(
    MethodParameter parameter,
    String name,
    Source source,
    List<String> values,
    String fallback
  ) {
    final Class<?> type = parameter.getParameterType();
    if (Collection.class.isAssignableFrom(type)) {
      final Class<?> element = elementType(parameter.getGenericParameterType());
      final List<String> text = values.isEmpty() && fallback != null
        ? Collections.singletonList(fallback)
        : values;
      final Collection<Object> read = Set.class.isAssignableFrom(type)
        ? new LinkedHashSet<>()
        : new ArrayList<>();
      for (String value : text) {
        read.add(ParamConverters.convert(value, element, name, source));
      }
      return read;
    }
    if (type == Optional.class) {
      final Class<?> element = elementType(parameter.getGenericParameterType());
      final String value = values.isEmpty() ? fallback : values.get(0);
      return value == null || value.isEmpty()
        ? Optional.empty()
        : Optional.ofNullable(ParamConverters.convert(value, element, name, source));
    }
    if (type == com.google.common.base.Optional.class) {
      final Class<?> element = elementType(parameter.getGenericParameterType());
      final String value = values.isEmpty() ? fallback : values.get(0);
      return value == null || value.isEmpty()
        ? com.google.common.base.Optional.absent()
        : com.google.common.base.Optional.fromNullable(
          ParamConverters.convert(value, element, name, source)
        );
    }
    final String value = values.isEmpty() ? fallback : values.get(0);
    if (value == null) {
      return type.isPrimitive() ? primitiveDefault(type) : null;
    }
    return ParamConverters.convert(value, type, name, source);
  }

  private static Class<?> elementType(Type type) {
    if (type instanceof ParameterizedType) {
      final Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
      if (arguments.length == 1 && arguments[0] instanceof Class) {
        return (Class<?>) arguments[0];
      }
    }
    return String.class;
  }

  /** What a parameter of a type that cannot hold nothing is read as when the request carries no value. */
  private static Object primitiveDefault(Class<?> type) {
    if (type == boolean.class) {
      return Boolean.FALSE;
    }
    if (type == int.class) {
      return Integer.valueOf(0);
    }
    if (type == long.class) {
      return Long.valueOf(0L);
    }
    if (type == double.class) {
      return Double.valueOf(0d);
    }
    if (type == float.class) {
      return Float.valueOf(0f);
    }
    if (type == short.class) {
      return Short.valueOf((short) 0);
    }
    if (type == byte.class) {
      return Byte.valueOf((byte) 0);
    }
    return Character.valueOf('\0');
  }
}
