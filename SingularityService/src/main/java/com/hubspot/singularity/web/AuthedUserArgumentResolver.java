package com.hubspot.singularity.web;

import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.authenticator.SingularityMultiMethodAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Works out the user a request is answered for, the way the service's configuration says a request may name
 * one, and hands it to the resource method that asked for it.
 */
public class AuthedUserArgumentResolver implements HandlerMethodArgumentResolver {
  private final SingularityMultiMethodAuthenticator authenticator;

  public AuthedUserArgumentResolver(
    SingularityMultiMethodAuthenticator authenticator
  ) {
    this.authenticator = authenticator;
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return (
      parameter.hasParameterAnnotation(Auth.class) &&
      SingularityUser.class.isAssignableFrom(parameter.getParameterType())
    );
  }

  @Override
  public Object resolveArgument(
    MethodParameter parameter,
    ModelAndViewContainer mavContainer,
    NativeWebRequest webRequest,
    WebDataBinderFactory binderFactory
  ) {
    final HttpServletRequest request = webRequest.getNativeRequest(
      HttpServletRequest.class
    );
    final Optional<SingularityUser> user = authenticator.authenticate(
      new ServletRequestContext(request)
    );
    return user.orElse(SingularityUser.DEFAULT_USER);
  }
}
