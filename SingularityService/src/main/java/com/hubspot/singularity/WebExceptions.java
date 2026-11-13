package com.hubspot.singularity;

import static java.lang.String.format;

import com.hubspot.singularity.web.SingularityWebException;
import com.hubspot.singularity.web.StatusReasons;

public final class WebExceptions {

  private WebExceptions() {}

  public static void checkBadRequest(boolean condition, String message, Object... args) {
    if (!condition) {
      badRequest(message, args);
    }
  }

  public static void checkConflict(boolean condition, String message, Object... args) {
    if (!condition) {
      conflict(message, args);
    }
  }

  public static void checkNotFound(boolean condition, String message, Object... args) {
    if (!condition) {
      notFound(message, args);
    }
  }

  public static void checkForbidden(boolean condition, String message, Object... args) {
    if (!condition) {
      forbidden(message, args);
    }
  }

  public static void checkUnauthorized(
    boolean condition,
    String message,
    Object... args
  ) {
    if (!condition) {
      unauthorized(message, args);
    }
  }

  public static void checkRateLimited(boolean condition, String message, Object... args) {
    if (!condition) {
      rateLimited(message, args);
    }
  }

  public static <T> T checkNotNullBadRequest(T value, String message, Object... args) {
    if (value == null) {
      badRequest(message, args);
    }
    return value;
  }

  public static SingularityWebException badRequest(String message, Object... args) {
    throw webException(400, message, args);
  }

  public static SingularityWebException timeout(String message, Object... args) {
    throw webException(408, message, args);
  }

  public static SingularityWebException conflict(String message, Object... args) {
    if (args.length > 0) {
      message = format(message, args);
    }
    throw new SingularityWebException(409, message);
  }

  public static SingularityWebException notFound(String message, Object... args) {
    if (args.length > 0) {
      message = format(message, args);
    }
    throw new SingularityWebException(404, message);
  }

  public static SingularityWebException forbidden(String message, Object... args) {
    return webException(403, message, args);
  }

  public static SingularityWebException unauthorized(String message, Object... args) {
    return webException(401, message, args);
  }

  public static SingularityWebException rateLimited(String message, Object... args) {
    return webException(429, message, args);
  }

  /**
   * A refusal the service builds out of a status: what the service says about it is kept, and the answer
   * carries the status and its reason, which is what the API has always answered for these.
   */
  private static SingularityWebException webException(
    int statusCode,
    String message,
    Object... formatArgs
  ) {
    if (formatArgs.length > 0) {
      message = format(message, formatArgs);
    }

    throw new SingularityWebException(
      statusCode,
      StatusReasons.message(statusCode),
      message
    );
  }
}
