package com.hubspot.singularity.web;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;

/**
 * What the connector writes for a request it refuses itself, before the service is given it.
 *
 * The service has always answered two kinds of such refusal differently. A request the connector could not
 * read at all - a target with an escape that is not one, a request line it cannot make sense of - is
 * answered with the short notice that the message was bad. A request the connector read but would not pass
 * on is answered with the page that names the target, and that answer says it must not be kept. Which of the
 * two it is, is whether the service was ever given the request.
 */
public class ContainerErrorValve extends ErrorReportValve {
  /** Marks a request the service itself was given, which the first of its filters sets. */
  public static final String SEEN = ContainerErrorValve.class.getName() + ".seen";

  private static final String BAD_MESSAGE =
    "<h1>Bad Message %1$d</h1><pre>reason: %2$s</pre>";

  @Override
  protected void report(Request request, Response response, Throwable throwable) {
    final int status = response.getStatus();
    if (status < 400 || response.getContentWritten() > 0 || !response.setErrorReported()) {
      return;
    }
    try {
      if (!Boolean.TRUE.equals(request.getAttribute(SEEN)) && status == 400) {
        badMessage(request, response, status);
      } else {
        ContainerErrorPage.write(request, response, status, target(request));
      }
    } catch (IOException | IllegalStateException e) {
      // nothing more can be written to an answer that is already on its way
    }
  }

  /** The short notice for a request the connector could not read. */
  private void badMessage(HttpServletRequest request, Response response, int status)
    throws IOException {
    final String reason = StatusReasons.reason(status);
    final byte[] entity = String
      .format(BAD_MESSAGE, status, reason == null ? "" : reason)
      .getBytes(StandardCharsets.ISO_8859_1);
    response.setStatus(status);
    response.setHeader("Content-Type", "text/html;charset=iso-8859-1");
    response.setContentLength(entity.length);
    response.getOutputStream().write(entity);
    response.getOutputStream().flush();
  }

  /** The target the page names, which is the one the request asked for. */
  private static String target(HttpServletRequest request) {
    final Object original = request.getAttribute("jakarta.servlet.error.request_uri");
    if (original != null) {
      return original.toString();
    }
    return request.getRequestURI() == null ? "/" : request.getRequestURI();
  }
}
