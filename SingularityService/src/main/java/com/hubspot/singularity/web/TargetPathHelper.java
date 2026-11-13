package com.hubspot.singularity.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UrlPathHelper;

/**
 * The target a resource is looked for by: the one the request was sent with.
 *
 * The service has always matched a target as it arrived. The parameters a segment carries are left out,
 * because they are not part of the target, and anything after a fragment marker is left out, because it
 * never reaches a server; nothing else is changed. An empty segment is kept, so a target that carries one
 * matches no resource, and an escape is kept as it was written, so a target whose escapes spell the letters
 * of a resource's own target does not reach that resource.
 */
public class TargetPathHelper extends UrlPathHelper {

  public TargetPathHelper() {
    setUrlDecode(false);
    setAlwaysUseFullPath(true);
    setRemoveSemicolonContent(true);
  }

  @Override
  public String getRequestUri(HttpServletRequest request) {
    final Object included = request.getAttribute(
      "jakarta.servlet.include.request_uri"
    );
    String uri = included instanceof String
      ? (String) included
      : request.getRequestURI();
    if (uri == null) {
      return "/";
    }
    final int fragment = uri.indexOf('#');
    if (fragment >= 0) {
      uri = uri.substring(0, fragment);
    }
    final int query = uri.indexOf('?');
    if (query >= 0) {
      uri = uri.substring(0, query);
    }
    return withoutSegmentParameters(uri);
  }

  /** The target without the parameters its segments carry, which are not part of it. */
  private static String withoutSegmentParameters(String uri) {
    if (uri.indexOf(';') < 0) {
      return uri;
    }
    final StringBuilder kept = new StringBuilder(uri.length());
    int at = 0;
    while (at < uri.length()) {
      final int slash = uri.indexOf('/', at);
      final int end = slash < 0 ? uri.length() : slash;
      final String segment = uri.substring(at, end);
      final int semicolon = segment.indexOf(';');
      kept.append(semicolon < 0 ? segment : segment.substring(0, semicolon));
      if (slash < 0) {
        break;
      }
      kept.append('/');
      at = slash + 1;
    }
    return kept.toString();
  }
}
