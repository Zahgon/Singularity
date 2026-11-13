package com.hubspot.singularity.web;

import com.hubspot.singularity.config.CorsConfiguration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * The cross-origin headers the service answers with.
 *
 * The service has always set this filter up twice over: it builds the filter and starts it with no settings
 * at all, so that the filter takes the ones it has by default, and then registers it with the settings its
 * configuration file names, which starts it a second time. The filter adds to the lists it already holds
 * rather than replacing them, so what a preflight is told is **both lists, one after the other**: the
 * methods it may use are the three of the default list followed by the six the configuration names, and the
 * headers it may send are the four of the default list followed by the five the configuration names. A
 * preflight may be cached for half an hour, credentials are allowed, no header is exposed, and a preflight
 * is answered by the resource as well as by the filter.
 */
public class CrossOriginFilter extends OncePerRequestFilter {
  private static final String ORIGIN_HEADER = "Origin";
  private static final String ACCESS_CONTROL_REQUEST_METHOD_HEADER =
    "Access-Control-Request-Method";
  private static final String ACCESS_CONTROL_REQUEST_HEADERS_HEADER =
    "Access-Control-Request-Headers";
  private static final String ACCESS_CONTROL_ALLOW_ORIGIN_HEADER =
    "Access-Control-Allow-Origin";
  private static final String ACCESS_CONTROL_ALLOW_METHODS_HEADER =
    "Access-Control-Allow-Methods";
  private static final String ACCESS_CONTROL_ALLOW_HEADERS_HEADER =
    "Access-Control-Allow-Headers";
  private static final String ACCESS_CONTROL_MAX_AGE_HEADER = "Access-Control-Max-Age";
  private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER =
    "Access-Control-Allow-Credentials";

  private static final List<String> SIMPLE_HTTP_METHODS = Arrays.asList(
    "GET",
    "POST",
    "HEAD"
  );
  // The lists the filter holds when it is started with no settings at all.
  private static final List<String> DEFAULT_METHODS = Arrays.asList("GET", "POST", "HEAD");
  private static final List<String> DEFAULT_HEADERS = Arrays.asList(
    "X-Requested-With",
    "Content-Type",
    "Accept",
    "Origin"
  );
  private static final int PREFLIGHT_MAX_AGE = 1800;

  private final List<String> allowedMethods;
  private final List<String> allowedHeaders;

  public CrossOriginFilter(CorsConfiguration cors) {
    this.allowedMethods = both(DEFAULT_METHODS, cors.getAllowedMethods());
    this.allowedHeaders = both(DEFAULT_HEADERS, cors.getAllowedHeaders());
  }

  /** What the filter holds after it is started a second time: what it held, and what it is given. */
  private static List<String> both(List<String> held, String given) {
    final List<String> all = new ArrayList<>(held);
    for (String value : given.split(",")) {
      final String trimmed = value.trim();
      if (!trimmed.isEmpty()) {
        all.add(trimmed);
      }
    }
    return all;
  }

  @Override
  protected void doFilterInternal(
    HttpServletRequest request,
    HttpServletResponse response,
    FilterChain chain
  )
    throws ServletException, IOException {
    final String origin = request.getHeader(ORIGIN_HEADER);
    if (origin != null && isEnabled(request)) {
      if (isSimpleRequest(request) || !isPreflightRequest(request)) {
        simpleResponse(response, origin);
      } else {
        preflightResponse(request, response, origin);
      }
    }
    chain.doFilter(request, response);
  }

  /** A client upgrading the connection takes no extra headers on the answer that upgrades it. */
  private boolean isEnabled(HttpServletRequest request) {
    for (
      Enumeration<String> connections = request.getHeaders("Connection");
      connections.hasMoreElements();
    ) {
      if ("Upgrade".equalsIgnoreCase(connections.nextElement())) {
        for (
          Enumeration<String> upgrades = request.getHeaders("Upgrade");
          upgrades.hasMoreElements();
        ) {
          if ("WebSocket".equalsIgnoreCase(upgrades.nextElement())) {
            return false;
          }
        }
      }
    }
    return true;
  }

  private boolean isSimpleRequest(HttpServletRequest request) {
    return (
      SIMPLE_HTTP_METHODS.contains(request.getMethod()) &&
      request.getHeader(ACCESS_CONTROL_REQUEST_METHOD_HEADER) == null
    );
  }

  private boolean isPreflightRequest(HttpServletRequest request) {
    return (
      "OPTIONS".equalsIgnoreCase(request.getMethod()) &&
      request.getHeader(ACCESS_CONTROL_REQUEST_METHOD_HEADER) != null
    );
  }

  private void simpleResponse(HttpServletResponse response, String origin) {
    response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, origin);
    response.addHeader("Vary", ORIGIN_HEADER);
    response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER, "true");
  }

  private void preflightResponse(
    HttpServletRequest request,
    HttpServletResponse response,
    String origin
  ) {
    final String method = request.getHeader(ACCESS_CONTROL_REQUEST_METHOD_HEADER);
    if (method == null || !allowedMethods.contains(method)) {
      return;
    }
    final List<String> requested = requestedHeaders(request);
    for (String header : requested) {
      boolean allowed = false;
      for (String candidate : allowedHeaders) {
        if (candidate.equalsIgnoreCase(header)) {
          allowed = true;
          break;
        }
      }
      if (!allowed) {
        return;
      }
    }
    response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN_HEADER, origin);
    response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS_HEADER, "true");
    response.setHeader(
      ACCESS_CONTROL_MAX_AGE_HEADER,
      String.valueOf(PREFLIGHT_MAX_AGE)
    );
    response.setHeader(ACCESS_CONTROL_ALLOW_METHODS_HEADER, commify(allowedMethods));
    response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS_HEADER, commify(allowedHeaders));
  }

  /** The headers a preflight asks about, read the way a list of values written on one line is read. */
  private List<String> requestedHeaders(HttpServletRequest request) {
    final String value = request.getHeader(ACCESS_CONTROL_REQUEST_HEADERS_HEADER);
    final List<String> headers = new ArrayList<>();
    if (value == null) {
      return headers;
    }
    for (String header : value.split(",")) {
      final String trimmed = header.trim();
      if (!trimmed.isEmpty()) {
        headers.add(trimmed);
      }
    }
    return headers;
  }

  private static String commify(List<String> values) {
    return String.join(",", values);
  }
}
