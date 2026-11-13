package com.hubspot.singularity.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;

/**
 * The page a connector writes for a request it refuses itself.
 *
 * The page names the status, the reason it is known by and the target that was asked for. It is only written
 * when the request will read a page: a request that says what it accepts and names neither pages nor
 * anything at all is answered with the status and nothing else. Either way the answer says it must not be
 * kept.
 */
public final class ContainerErrorPage {
  public static final String NO_CACHE = "must-revalidate,no-cache,no-store";
  private static final String PAGE =
    "<html>\n" +
    "<head>\n" +
    "<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\"/>\n" +
    "<title>Error %1$d %2$s</title>\n" +
    "</head>\n" +
    "<body><h2>HTTP ERROR %1$d</h2>\n" +
    "<p>Problem accessing %3$s. Reason:\n" +
    "<pre>    %2$s</pre></p>\n" +
    "</body>\n" +
    "</html>\n";

  private ContainerErrorPage() {}

  /** Writes the answer a connector gives for a request it refuses, for the target that was asked for. */
  public static void write(
    HttpServletRequest request,
    HttpServletResponse response,
    int status,
    String target
  )
    throws IOException {
    write(request, response, status, target, StatusReasons.reason(status));
  }

  public static void write(
    HttpServletRequest request,
    HttpServletResponse response,
    int status,
    String target,
    String reason
  )
    throws IOException {
    response.reset();
    response.setStatus(status);
    // A method whose answers never carry an entity of their own accord is answered with the status alone,
    // and such an answer says nothing about caching either.
    if (!writesPage(request)) {
      response.setContentLength(0);
      return;
    }
    response.setHeader("Cache-Control", NO_CACHE);
    if (!readsPage(request)) {
      response.setContentLength(0);
      return;
    }
    final byte[] entity = String
      .format(PAGE, status, reason == null ? "" : reason, target)
      .getBytes(StandardCharsets.ISO_8859_1);
    response.setHeader("Content-Type", "text/html;charset=iso-8859-1");
    response.setContentLength(entity.length);
    response.getOutputStream().write(entity);
    response.getOutputStream().flush();
  }

  /**
   * Whether a page is written for this request at all. The connector writes one only for the methods whose
   * answers carry an entity of their own accord, whatever the request asks for, and for a request that asks
   * for the headers alone it says how long the page would have been without sending it.
   */
  public static boolean writesPage(HttpServletRequest request) {
    final String method = request.getMethod();
    return (
      "GET".equalsIgnoreCase(method) ||
      "POST".equalsIgnoreCase(method) ||
      "HEAD".equalsIgnoreCase(method)
    );
  }

  /**
   * Whether the request will read a page. A request that says nothing about what it accepts will, and so
   * will one that accepts pages or anything; one that accepts only something else is answered with the
   * status alone, because the page is the only thing the connector knows how to write.
   */
  public static boolean readsPage(HttpServletRequest request) {
    final Enumeration<String> accepts = request.getHeaders("Accept");
    boolean said = false;
    while (accepts != null && accepts.hasMoreElements()) {
      final String value = accepts.nextElement();
      said = true;
      for (String entry : value.split(",")) {
        final String[] parts = entry.trim().split(";");
        final String media = parts[0].trim().toLowerCase(Locale.ENGLISH);
        if ("text/html".equals(media) || "text/*".equals(media) || "*/*".equals(media)) {
          return true;
        }
      }
    }
    return !said;
  }
}
