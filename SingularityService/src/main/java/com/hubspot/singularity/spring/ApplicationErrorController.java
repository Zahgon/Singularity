package com.hubspot.singularity.spring;

import com.hubspot.singularity.web.ErrorMessage;
import com.hubspot.singularity.web.StatusReasons;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * What the API's connector answers for a request it refuses before any resource is reached: the status, and
 * nothing else. The API has always answered those with no entity at all.
 */
@Controller
public class ApplicationErrorController implements ErrorController {

  @RequestMapping("${server.error.path:${error.path:/error}}")
  public ResponseEntity<Object> error(
    HttpServletRequest request,
    HttpServletResponse response
  )
    throws java.io.IOException {
    if (request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE) == null) {
      // the target the framework keeps for its own errors is not a target the service answers
      return ResponseEntity
        .status(404)
        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
        .body(new ErrorMessage(404, StatusReasons.message(404)));
    }
    return ResponseEntity.status(response.getStatus()).build();
  }
}
