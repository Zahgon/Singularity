package com.hubspot.singularity.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * A resource method that answers nothing at all is answered as no content, which is what the API has always
 * answered for one.
 */
public class VoidAnswerInterceptor implements HandlerInterceptor {
  @Override
  public void postHandle(
    HttpServletRequest request,
    HttpServletResponse response,
    Object handler,
    ModelAndView modelAndView
  ) {
    if (!(handler instanceof HandlerMethod)) {
      return;
    }
    final Class<?> type = ((HandlerMethod) handler).getReturnType().getParameterType();
    if ((type == void.class || type == Void.class) && response.getStatus() == 200) {
      response.setStatus(204);
    }
  }
}
