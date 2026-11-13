package com.hubspot.singularity.web;

import com.hubspot.singularity.jackson.FuzzyEnums;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Reading the value of a parameter out of the text a request carries it as.
 *
 * Which refusal an unreadable value earns depends on where the value was read from and on what it was being
 * read as, and the service has always answered them that way. A value in the path that cannot be read is
 * answered as though the path named nothing; a value in the query or in a header that cannot be read is
 * refused as a bad request; and an enumerated value that cannot be read is refused as a bad request that
 * says which values the parameter takes, wherever it was read from.
 */
public final class ParamConverters {

  private ParamConverters() {}

  public static Object convert(String value, Class<?> type, String name, Source source) {
    return read(value, type, source.names(name), source);
  }

  private static Object read(String value, Class<?> type, String named, Source source) {
    if (type == String.class) {
      return value;
    }
    if (type.isEnum()) {
      return convertEnum(value, type, named);
    }
    try {
      if (type == Boolean.class || type == boolean.class) {
        return Boolean.valueOf(value);
      }
      if (type == Integer.class || type == int.class) {
        return Integer.valueOf(value);
      }
      if (type == Long.class || type == long.class) {
        return Long.valueOf(value);
      }
      if (type == Double.class || type == double.class) {
        return Double.valueOf(value);
      }
      if (type == Float.class || type == float.class) {
        return Float.valueOf(value);
      }
      if (type == Short.class || type == short.class) {
        return Short.valueOf(value);
      }
      if (type == Byte.class || type == byte.class) {
        return Byte.valueOf(value);
      }
      if (type == Character.class || type == char.class) {
        return value.length() == 1 ? Character.valueOf(value.charAt(0)) : null;
      }
      final Method fromString = fromStringMethod(type);
      if (fromString != null) {
        return fromString.invoke(null, value);
      }
      return type.getConstructor(String.class).newInstance(value);
    } catch (ReflectiveOperationException | IllegalArgumentException e) {
      if (e instanceof InvocationTargetException) {
        final Throwable cause = ((InvocationTargetException) e).getCause();
        if (cause instanceof SingularityWebException) {
          throw (SingularityWebException) cause;
        }
      }
      throw source.refuse();
    }
  }

  /**
   * An enumerated value, read permissively. A type that brings a reader of its own is asked first, and what
   * it refuses is refused as a value that is not of that type.
   */
  private static Object convertEnum(String value, Class<?> type, String name) {
    final Enum<?>[] constants = (Enum<?>[]) type.getEnumConstants();
    final Method fromString = fromStringMethod(type);
    if (fromString != null) {
      try {
        final Object constant = fromString.invoke(null, value);
        if (constant != null) {
          return constant;
        }
        throw badRequest(name + " is not a valid " + type.getSimpleName());
      } catch (IllegalAccessException e) {
        throw badRequest(
          "Not permitted to call fromString on " + type.getSimpleName()
        );
      } catch (InvocationTargetException e) {
        if (e.getCause() instanceof SingularityWebException) {
          throw (SingularityWebException) e.getCause();
        }
        throw badRequest("Failed to convert " + name + " to " + type.getSimpleName());
      }
    }
    final Object constant = FuzzyEnums.fromStringFuzzy(value, constants);
    if (constant != null) {
      return constant;
    }
    throw badRequest(
      name + " must be one of [" + FuzzyEnums.listConstants(constants) + "]"
    );
  }

  private static Method fromStringMethod(Class<?> type) {
    try {
      final Method method = type.getMethod("fromString", String.class);
      return Modifier.isStatic(method.getModifiers()) ? method : null;
    } catch (NoSuchMethodException e) {
      return null;
    }
  }

  static SingularityWebException badRequest(String message) {
    return new SingularityWebException(400, message);
  }

  /** Where a value was read from, which decides how an unreadable one is refused. */
  public enum Source {
    PATH,
    QUERY,
    HEADER;

    /** What a refusal calls a parameter read from here: where it was read from, and its name. */
    String names(String name) {
      switch (this) {
        case PATH:
          return "path param " + name;
        case QUERY:
          return "query param " + name;
        default:
          return "header " + name;
      }
    }

    /**
     * A value of the target that cannot be read is answered as though the target named nothing, because it
     * is part of the target; a value of a header that cannot be read is a bad request.
     */
    SingularityWebException refuse() {
      return this == HEADER
        ? new SingularityWebException(400, StatusReasons.message(400))
        : new SingularityWebException(404, StatusReasons.message(404));
    }
  }
}
