package com.hubspot.singularity.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Set;

/**
 * Answers a request that asks what a target allows.
 *
 * The answer names the methods the target answers, and carries them as text when the request will read text,
 * and nothing at all when it will only read something else. A target that answers nothing is left to be
 * answered as a target no resource claims.
 */
public class OptionsFilter extends org.springframework.web.filter.OncePerRequestFilter {
  private static final String PLAIN_TEXT = "text/plain";
  private static final String JSON = "application/json";

  private final AllowedMethods allowed;

  public OptionsFilter(AllowedMethods allowed) {
    this.allowed = allowed;
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
  )
    throws ServletException, IOException {
    if (!"OPTIONS".equals(request.getMethod())) {
      chain.doFilter(request, response);
      return;
    }
    final Set<String> methods = allowed.forOptions(request);
    if (methods.isEmpty()) {
      chain.doFilter(request, response);
      return;
    }
    response.setStatus(200);
    response.setHeader("Allow", AllowedMethods.header(methods));
    if (readsText(request)) {
      final byte[] entity = AllowedMethods.entity(methods).getBytes(StandardCharsets.UTF_8);
      response.setHeader("Content-Type", PLAIN_TEXT);
      response.getOutputStream().write(entity);
    } else {
      response.setHeader("Content-Type", JSON);
      response.setContentLength(0);
    }
  }

  /**
   * Whether the request will read the list as text. A request that says nothing about what it accepts will,
   * and so will one that accepts anything; one that accepts only something else is answered with nothing.
   */
  private static boolean readsText(HttpServletRequest request) {
    final Enumeration<String> accepts = request.getHeaders("Accept");
    boolean said = false;
    while (accepts != null && accepts.hasMoreElements()) {
      final String value = accepts.nextElement();
      for (String entry : value.split(",")) {
        final String[] parts = entry.trim().split(";");
        final String media = parts[0].trim().toLowerCase(Locale.ENGLISH);
        if (media.isEmpty()) {
          continue;
        }
        said = true;
        if (PLAIN_TEXT.equals(media) || "text/*".equals(media) || "*/*".equals(media)) {
          return true;
        }
      }
    }
    return !said;
  }
}
