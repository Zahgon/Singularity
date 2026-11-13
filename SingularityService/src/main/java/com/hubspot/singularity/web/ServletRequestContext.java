package com.hubspot.singularity.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * The request being answered, read the way the service has always read one.
 */
public class ServletRequestContext implements SingularityRequestContext {
  private final HttpServletRequest request;

  public ServletRequestContext(HttpServletRequest request) {
    this.request = request;
  }

  public HttpServletRequest getRequest() {
    return request;
  }

  @Override
  public String getHeaderString(String name) {
    final Enumeration<String> values = request.getHeaders(name);
    if (values == null || !values.hasMoreElements()) {
      return null;
    }
    final StringBuilder joined = new StringBuilder(values.nextElement());
    while (values.hasMoreElements()) {
      joined.append(',').append(values.nextElement());
    }
    return joined.toString();
  }

  @Override
  public String getFirstQueryParameter(String name) {
    final String[] values = QueryParameters.of(request).get(name);
    return values == null || values.length == 0 ? null : values[0];
  }
}
