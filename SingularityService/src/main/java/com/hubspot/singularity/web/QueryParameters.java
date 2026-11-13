package com.hubspot.singularity.web;

import jakarta.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The values a query carries, read out of the text of the query itself.
 *
 * The query is the only place the service reads these from: an entity the request carries is never read as
 * though it were a query, however it is written. A name that carries no equals sign carries the empty value,
 * a name written more than once keeps every value in the order the query writes them, the escapes are read
 * as UTF-8, and a plus sign stands for a space.
 */
public final class QueryParameters {
  private static final Map<String, String[]> NONE = new LinkedHashMap<>();
  private static final String ATTRIBUTE = QueryParameters.class.getName();

  private QueryParameters() {}

  /** The values the query of this request carries, worked out once per request and kept. */
  @SuppressWarnings("unchecked")
  public static Map<String, String[]> of(HttpServletRequest request) {
    final Object cached = request.getAttribute(ATTRIBUTE);
    if (cached instanceof Map) {
      return (Map<String, String[]>) cached;
    }
    final Map<String, String[]> parsed = parse(request.getQueryString());
    request.setAttribute(ATTRIBUTE, parsed);
    return parsed;
  }

  /** The first value the query of this request carries under this name. */
  public static String first(HttpServletRequest request, String name) {
    final String[] values = of(request).get(name);
    return values == null || values.length == 0 ? null : values[0];
  }

  /** Every value the query of this request carries under this name, in the order it writes them. */
  public static List<String> all(HttpServletRequest request, String name) {
    final String[] values = of(request).get(name);
    final List<String> all = new ArrayList<>();
    if (values != null) {
      for (String value : values) {
        all.add(value);
      }
    }
    return all;
  }

  public static Map<String, String[]> parse(String query) {
    if (query == null || query.isEmpty()) {
      return NONE;
    }
    final Map<String, List<String>> values = new LinkedHashMap<>();
    for (String pair : query.split("&", -1)) {
      if (pair.isEmpty()) {
        continue;
      }
      final int equals = pair.indexOf('=');
      final String name = equals < 0 ? pair : pair.substring(0, equals);
      final String value = equals < 0 ? "" : pair.substring(equals + 1);
      values
        .computeIfAbsent(decode(name), key -> new ArrayList<>())
        .add(decode(value));
    }
    final Map<String, String[]> parsed = new LinkedHashMap<>();
    for (Map.Entry<String, List<String>> entry : values.entrySet()) {
      parsed.put(entry.getKey(), entry.getValue().toArray(new String[0]));
    }
    return parsed;
  }

  private static String decode(String value) {
    try {
      return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException | IllegalArgumentException e) {
      return value;
    }
  }
}
