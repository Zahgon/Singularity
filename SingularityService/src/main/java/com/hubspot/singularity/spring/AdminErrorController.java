package com.hubspot.singularity.spring;

import com.hubspot.singularity.web.ContainerErrorPage;
import com.hubspot.singularity.web.StatusReasons;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * The page the administrative connector answers for a request it refuses: the page the connector has always
 * written, which names the target that was asked for and the reason the status is known by.
 */
@Controller
public class AdminErrorController implements ErrorController {
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

  @RequestMapping("${server.error.path:${error.path:/error}}")
  public ResponseEntity<String> error(
    HttpServletRequest request,
    HttpServletResponse response
  )
    throws java.io.IOException {
    if (request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) == null) {
      // the target the framework keeps for its own errors is not a target the service answers
      ContainerErrorPage.write(request, response, 404, request.getRequestURI());
      return null;
    }
    final int status = response.getStatus();
    final String reason = reason(status);
    final Object target = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    return ResponseEntity
      .status(status)
      .header(HttpHeaders.CACHE_CONTROL, "must-revalidate,no-cache,no-store")
      .header(HttpHeaders.CONTENT_TYPE, "text/html;charset=iso-8859-1")
      .body(
        String.format(
          PAGE,
          status,
          reason,
          target == null ? request.getRequestURI() : target.toString()
        )
      );
  }

  /** The reason the status is known by, which is what the page has always named. */
  private static String reason(int status) {
    final String reason = StatusReasons.reason(status);
    return reason == null ? "" : reason;
  }
}
