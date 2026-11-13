package com.hubspot.singularity.spring;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.hubspot.singularity.web.AllowedMethods;
import com.hubspot.singularity.web.ErrorMessage;
import com.hubspot.singularity.web.SingularityWebException;
import com.hubspot.singularity.web.StatusReasons;
import com.hubspot.singularity.web.ValidationErrorMessage;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * What the API answers when a request cannot be answered.
 *
 * A refusal the service decided on carries the status it decided on and the text that refusal carries. A
 * request the service could not read at all carries the refusal the reading of it earned. Anything else is
 * an error of the service: it is written to the log under an identifier, and the answer carries that
 * identifier rather than what went wrong.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SingularityExceptionHandler {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityExceptionHandler.class
  );

  private final AllowedMethods allowed;

  public SingularityExceptionHandler(AllowedMethods allowed) {
    this.allowed = allowed;
  }

  @ExceptionHandler(SingularityWebException.class)
  public ResponseEntity<Object> refusal(SingularityWebException exception) {
    if (exception.getStatus() / 100 == 3) {
      final ResponseEntity.BodyBuilder redirect = ResponseEntity.status(
        exception.getStatus()
      );
      exception.getHeaders().forEach(redirect::header);
      return redirect.build();
    }
    if (exception.getStatus() / 100 == 5) {
      logException(exception);
    }
    final ResponseEntity.BodyBuilder answer = ResponseEntity
      .status(exception.getStatus())
      .contentType(MediaType.APPLICATION_JSON);
    exception.getHeaders().forEach(answer::header);
    return answer.body(new ErrorMessage(exception.getStatus(), exception.getMessage()));
  }

  @ExceptionHandler(NoHandlerFoundException.class)
  public ResponseEntity<Object> notFound(NoHandlerFoundException exception) {
    return error(404);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<Object> methodNotAllowed(
    HttpServletRequest request,
    HttpRequestMethodNotSupportedException exception
  ) {
    final ResponseEntity.BodyBuilder answer = ResponseEntity
      .status(405)
      .contentType(MediaType.APPLICATION_JSON);
    final Set<String> methods = allowed.forRefusal(request);
    if (!methods.isEmpty()) {
      answer.header(HttpHeaders.ALLOW, AllowedMethods.header(methods));
    }
    return answer.body(new ErrorMessage(405, StatusReasons.message(405)));
  }

  @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
  public ResponseEntity<Object> notAcceptable(
    HttpMediaTypeNotAcceptableException exception
  ) {
    return error(406);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<Object> unsupportedMediaType(
    HttpMediaTypeNotSupportedException exception
  ) {
    return error(415);
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<Object> badParameter(
    MethodArgumentTypeMismatchException exception
  ) {
    return error(400);
  }

  /** An entity the service could not read as JSON is the sender's error, and says only that much. */
  @ExceptionHandler(
    { JsonProcessingException.class, org.springframework.http.converter.HttpMessageNotReadableException.class }
  )
  public ResponseEntity<Object> unreadableEntity(Exception exception) {
    final Throwable cause = exception instanceof JsonProcessingException
      ? exception
      : exception.getCause();
    if (
      cause instanceof JsonGenerationException ||
      cause instanceof InvalidDefinitionException
    ) {
      return unexpected(exception);
    }
    LOG.debug("Unable to process JSON", exception);
    return ResponseEntity
      .status(400)
      .contentType(MediaType.APPLICATION_JSON)
      .body(new ErrorMessage(400, "Unable to process JSON"));
  }

  /** An entity that breaks the constraints of what it is read as lists the constraints it breaks. */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Object> invalidEntity(ConstraintViolationException exception) {
    final List<String> errors = new ArrayList<>();
    for (ConstraintViolation<?> violation : exception.getConstraintViolations()) {
      errors.add(violation.getPropertyPath() + " " + violation.getMessage());
    }
    return ResponseEntity
      .status(422)
      .contentType(MediaType.APPLICATION_JSON)
      .body(new ValidationErrorMessage(errors));
  }

  @ExceptionHandler(Throwable.class)
  public ResponseEntity<Object> unexpected(Throwable exception) {
    final long id = logException(exception);
    return ResponseEntity
      .status(HttpStatus.INTERNAL_SERVER_ERROR)
      .contentType(MediaType.APPLICATION_JSON)
      .body(
        new ErrorMessage(
          500,
          String.format(
            "There was an error processing your request. It has been logged (ID %016x).",
            id
          )
        )
      );
  }

  private static ResponseEntity<Object> error(int status) {
    return ResponseEntity
      .status(status)
      .contentType(MediaType.APPLICATION_JSON)
      .body(new ErrorMessage(status, StatusReasons.message(status)));
  }

  private static long logException(Throwable exception) {
    final long id = ThreadLocalRandom.current().nextLong();
    LOG.error(String.format("Error handling a request: %016x", id), exception);
    return id;
  }

}
