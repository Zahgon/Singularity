package com.hubspot.singularity.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses a request whose method is not one the service answers at all, before anything of the service sees
 * it. Which methods those are is what the server block of the configuration file says, and by default they
 * are the seven the service has always answered.
 *
 * The refusal is the connector's own answer, so it carries the page the connector writes for one, which it
 * only writes for the methods whose answers carry an entity of their own accord.
 */
public class AllowedMethodsFilter extends OncePerRequestFilter {
  private final Set<String> allowedMethods;

  public AllowedMethodsFilter(Collection<String> allowedMethods) {
    this.allowedMethods = new LinkedHashSet<>(allowedMethods);
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
  )
    throws ServletException, IOException {
    final String method = request.getMethod();
    if (allowedMethods.contains(method)) {
      chain.doFilter(request, response);
      return;
    }
    ContainerErrorPage.write(request, response, 405, request.getRequestURI());
  }
}
