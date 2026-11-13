package com.hubspot.singularity.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * A resource method that answers with nothing where it could have answered with something is answered as a
 * target that named nothing, which is what the API has always answered for one. A method that does answer
 * with something is left to be written the way every other answer is written.
 */
public class EmptyOptionalReturnHandler implements HandlerMethodReturnValueHandler {
  private final HandlerMethodReturnValueHandler delegate;

  public EmptyOptionalReturnHandler(HandlerMethodReturnValueHandler delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean supportsReturnType(MethodParameter returnType) {
    final Class<?> type = returnType.getParameterType();
    return (
      (Optional.class.equals(type) ||
        com.google.common.base.Optional.class.equals(type)) &&
      delegate.supportsReturnType(returnType)
    );
  }

  @Override
  public void handleReturnValue(
    Object returnValue,
    MethodParameter returnType,
    ModelAndViewContainer mavContainer,
    NativeWebRequest webRequest
  )
    throws Exception {
    final boolean empty =
      returnValue == null ||
      (returnValue instanceof Optional && !((Optional<?>) returnValue).isPresent()) ||
      (returnValue instanceof com.google.common.base.Optional &&
        !((com.google.common.base.Optional<?>) returnValue).isPresent());
    if (!empty) {
      delegate.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
      return;
    }
    final HttpServletRequest request = webRequest.getNativeRequest(
      HttpServletRequest.class
    );
    final HttpServletResponse response = webRequest.getNativeResponse(
      HttpServletResponse.class
    );
    mavContainer.setRequestHandled(true);
    ContainerErrorPage.write(request, response, 404, request.getRequestURI());
  }
}
