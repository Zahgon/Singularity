package com.hubspot.singularity.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * The methods a target answers, and the order the service lists them in.
 *
 * The list is worked out from the resources themselves, and the order is the one the set the service keeps
 * them in gives them, which is neither the order they are declared in nor an alphabetical one. A target that
 * answers a GET answers a request for its headers as well, and every target that answers anything answers a
 * request for what it allows.
 */
public class AllowedMethods {
  private final RequestMappingHandlerMapping mapping;
  private final AntPathMatcher matcher = new AntPathMatcher();

  public AllowedMethods(RequestMappingHandlerMapping mapping) {
    this.mapping = mapping;
  }

  /** The methods the resources declare for the target of this request, as they declare them. */
  public Set<String> declared(HttpServletRequest request) {
    final String path = path(request);
    // The resource that claims a target is the one whose own target is closest to it, and the methods it
    // declares are the only ones the target answers; a resource whose target is less close does not add to
    // them, however many methods it declares.
    String best = null;
    final Comparator<String> closest = matcher.getPatternComparator(path);
    final Set<String> methods = new HashSet<>();
    for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : mapping
      .getHandlerMethods()
      .entrySet()) {
      for (String pattern : patterns(entry.getKey())) {
        if (!pattern.equals(path) && !matcher.match(pattern, path)) {
          continue;
        }
        if (best == null || closest.compare(pattern, best) < 0) {
          best = pattern;
          methods.clear();
        }
        if (!pattern.equals(best)) {
          continue;
        }
        final RequestMethodsRequestCondition condition = entry
          .getKey()
          .getMethodsCondition();
        if (condition.getMethods().isEmpty()) {
          methods.add("GET");
          methods.add("POST");
          methods.add("PUT");
          methods.add("DELETE");
        } else {
          condition.getMethods().forEach(method -> methods.add(method.name()));
        }
      }
    }
    return methods;
  }

  /** What a refusal of a method says is allowed: what the target declares, and asking what it allows. */
  public Set<String> forRefusal(HttpServletRequest request) {
    final Set<String> methods = declared(request);
    if (methods.isEmpty()) {
      return methods;
    }
    final Set<String> allowed = new HashSet<>(methods);
    allowed.add("OPTIONS");
    return order(allowed);
  }

  /** What an answer to a request for what a target allows says: that, and asking for its headers. */
  public Set<String> forOptions(HttpServletRequest request) {
    final Set<String> methods = declared(request);
    if (methods.isEmpty()) {
      return methods;
    }
    final Set<String> allowed = new HashSet<>(methods);
    if (allowed.contains("GET")) {
      allowed.add("HEAD");
    }
    allowed.add("OPTIONS");
    return order(allowed);
  }

  /** The order the service lists the methods in, which is the order its own set holds them in. */
  private static Set<String> order(Set<String> methods) {
    return new LinkedHashSet<>(methods);
  }

  private boolean matches(RequestMappingInfo info, String path) {
    for (String pattern : patterns(info)) {
      if (pattern.equals(path) || matcher.match(pattern, path)) {
        return true;
      }
    }
    return false;
  }

  private static Set<String> patterns(RequestMappingInfo info) {
    final Set<String> patterns = new LinkedHashSet<>(info.getPatternValues());
    if (patterns.isEmpty()) {
      patterns.add("/");
    }
    return patterns;
  }

  /** The target of a request, as the resources name their own targets. */
  public static String path(HttpServletRequest request) {
    final String path = request.getPathInfo() == null
      ? request.getServletPath()
      : request.getPathInfo();
    return path == null || path.isEmpty() ? "/" : path;
  }

  /** The list as an answer writes it, which is the methods one after another with nothing between. */
  public static String header(Set<String> methods) {
    return String.join(",", methods);
  }

  /** The list as the entity of an answer writes it, which has a space after each comma. */
  public static String entity(Set<String> methods) {
    return String.join(", ", methods);
  }
}
