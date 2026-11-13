package com.hubspot.singularity.spring;

import com.hubspot.singularity.auth.authenticator.SingularityMultiMethodAuthenticator;
import com.hubspot.singularity.web.AuthedUserArgumentResolver;
import com.hubspot.singularity.web.AllowedMethods;
import com.hubspot.singularity.web.EmptyOptionalReturnHandler;
import com.hubspot.singularity.web.SingularityArgumentResolver;
import com.hubspot.singularity.web.TargetPathHelper;
import com.hubspot.singularity.web.VoidAnswerInterceptor;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

/**
 * How the dispatcher reads a request and writes an answer: the entity of an answer is JSON written by the
 * service's own mapper, text is written as it stands, the parameters of a resource method are read out of
 * the request the way the service has always read them, and the kind of entity an answer carries is decided
 * by what the request accepts alone, never by its target or by a parameter of the query.
 */
@Configuration(proxyBeanMethods = false)
public class SingularityMvcConfiguration implements WebMvcConfigurer {
  private final SingularityPlatform platform;

  public SingularityMvcConfiguration(SingularityPlatform platform) {
    this.platform = platform;
  }

  @Override
  public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
    final MappingJackson2HttpMessageConverter json = new MappingJackson2HttpMessageConverter(
      platform.getApiObjectMapper()
    );
    json.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON));
    json.setDefaultCharset(null);
    final StringHttpMessageConverter text = new StringHttpMessageConverter(
      StandardCharsets.UTF_8
    );
    text.setWriteAcceptCharset(false);
    // The entity of a request is read as JSON whenever the request says it is JSON, whatever the resource
    // method holds it in, which is how the service has always read one.
    converters.add(json);
    converters.add(new ByteArrayHttpMessageConverter());
    converters.add(text);
    converters.add(new ResourceHttpMessageConverter());
  }

  @Override
  public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    configurer.favorParameter(false).ignoreAcceptHeader(false);
  }

  /**
   * How a target is read when a resource is looked for: as it was sent, with the escapes left as they are
   * and the parameters of a segment left out. The service has always matched a target that way, so a target
   * whose escapes spell a letter of a resource's own target does not reach that resource, an empty segment
   * matches nothing, and a dot segment is not resolved away.
   */
  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.setUrlPathHelper(new TargetPathHelper());
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(new VoidAnswerInterceptor());
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new SingularityArgumentResolver());
    resolvers.add(
      new AuthedUserArgumentResolver(
        platform.getInjector().getInstance(SingularityMultiMethodAuthenticator.class)
      )
    );
  }

  /**
   * The answers of the resources are written by the framework's own writer, except that a method that
   * answered with nothing where it could have answered with something is answered as a target that named
   * nothing.
   */
  @Bean
  public org.springframework.beans.factory.config.BeanPostProcessor emptyOptionalAnswers() {
    return new org.springframework.beans.factory.config.BeanPostProcessor() {

      @Override
      public Object postProcessAfterInitialization(Object bean, String name) {
        if (!(bean instanceof RequestMappingHandlerAdapter)) {
          return bean;
        }
        final RequestMappingHandlerAdapter adapter = (RequestMappingHandlerAdapter) bean;
        final List<HandlerMethodReturnValueHandler> handlers = new java.util.ArrayList<>(
          adapter.getReturnValueHandlers() == null
            ? List.of()
            : adapter.getReturnValueHandlers()
        );
        for (int i = 0; i < handlers.size(); i++) {
          if (handlers.get(i) instanceof RequestResponseBodyMethodProcessor) {
            handlers.add(i, new EmptyOptionalReturnHandler(handlers.get(i)));
            break;
          }
        }
        adapter.setReturnValueHandlers(handlers);
        return bean;
      }
    };
  }

  /** The methods a target answers, worked out from the resources themselves. */
  @Bean
  public AllowedMethods allowedMethods(RequestMappingHandlerMapping mapping) {
    return new AllowedMethods(mapping);
  }
}
