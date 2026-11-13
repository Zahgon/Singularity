package com.hubspot.singularity.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the kinds of entity a request names, the way the service has always read them.
 *
 * A kind written as a bare name, with no subkind, means every subkind of that name. A kind whose name or
 * subkind is missing, or that carries a weight that is not a weight, is a request the service will not read
 * at all, and the connector gives up on it before any resource is reached. What is left is handed on with
 * the bare names written out, so that what an answer carries is decided from the same reading.
 */
public class MediaTypeFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
  )
    throws ServletException, IOException {
    request.setAttribute(ContainerErrorValve.SEEN, Boolean.TRUE);
    final String type = request.getHeader("Content-Type");
    if (type != null && !readable(type)) {
      ContainerErrorPage.write(request, response, 400, request.getRequestURI());
      return;
    }
    final List<String> accepts = new ArrayList<>();
    final Enumeration<String> values = request.getHeaders("Accept");
    while (values != null && values.hasMoreElements()) {
      final String value = values.nextElement();
      final String written = writtenOut(value);
      if (written == null) {
        ContainerErrorPage.write(request, response, 400, request.getRequestURI());
        return;
      }
      accepts.add(written);
    }
    chain.doFilter(accepts.isEmpty() ? request : new WithAccept(request, accepts), response);
  }

  /** Whether the kind of an entity is one the service will read: a name and a subkind, both of them there. */
  private static boolean readable(String type) {
    final String media = type.split(";")[0].trim();
    final int slash = media.indexOf('/');
    return slash > 0 && slash < media.length() - 1;
  }

  /**
   * The kinds a request accepts, with a bare name written out as every subkind of it, or nothing at all when
   * one of them is written in a way the service will not read.
   */
  private static String writtenOut(String value) {
    final List<String> entries = new ArrayList<>();
    for (String entry : value.split(",")) {
      final String trimmed = entry.trim();
      if (trimmed.isEmpty()) {
        continue;
      }
      final String[] parts = trimmed.split(";");
      final String media = parts[0].trim();
      if (media.isEmpty()) {
        return null;
      }
      final int slash = media.indexOf('/');
      if (slash == 0 || slash == media.length() - 1) {
        return null;
      }
      for (int i = 1; i < parts.length; i++) {
        final String parameter = parts[i].trim();
        final int equals = parameter.indexOf('=');
        if (equals <= 0 || equals == parameter.length() - 1) {
          return null;
        }
        final String name = parameter.substring(0, equals).trim().toLowerCase(Locale.ENGLISH);
        final String weight = parameter.substring(equals + 1).trim();
        if ("q".equals(name)) {
          try {
            final double q = Double.parseDouble(weight);
            if (q < 0 || q > 1) {
              return null;
            }
          } catch (NumberFormatException e) {
            return null;
          }
        }
      }
      entries.add(
        slash < 0
          ? media + "/*" + trimmed.substring(media.length())
          : trimmed
      );
    }
    return String.join(",", entries);
  }

  /** The request with the kinds it accepts written out. */
  private static final class WithAccept extends HttpServletRequestWrapper {
    private final List<String> accepts;

    WithAccept(HttpServletRequest request, List<String> accepts) {
      super(request);
      this.accepts = accepts;
    }

    @Override
    public String getHeader(String name) {
      return "accept".equalsIgnoreCase(name) ? accepts.get(0) : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
      return "accept".equalsIgnoreCase(name)
        ? Collections.enumeration(accepts)
        : super.getHeaders(name);
    }
  }
}
