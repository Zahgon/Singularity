package com.hubspot.singularity.web;

import java.util.HashMap;
import java.util.Map;

/**
 * The reason a status is known by, which is part of the text a refusal built out of a status alone carries.
 * A status the service has no reason for carries none.
 */
public final class StatusReasons {
  private static final Map<Integer, String> REASONS = new HashMap<>();

  static {
    REASONS.put(200, "OK");
    REASONS.put(201, "Created");
    REASONS.put(202, "Accepted");
    REASONS.put(204, "No Content");
    REASONS.put(205, "Reset Content");
    REASONS.put(206, "Partial Content");
    REASONS.put(300, "Multiple Choices");
    REASONS.put(301, "Moved Permanently");
    REASONS.put(302, "Found");
    REASONS.put(303, "See Other");
    REASONS.put(304, "Not Modified");
    REASONS.put(305, "Use Proxy");
    REASONS.put(307, "Temporary Redirect");
    REASONS.put(400, "Bad Request");
    REASONS.put(401, "Unauthorized");
    REASONS.put(402, "Payment Required");
    REASONS.put(403, "Forbidden");
    REASONS.put(404, "Not Found");
    REASONS.put(405, "Method Not Allowed");
    REASONS.put(406, "Not Acceptable");
    REASONS.put(407, "Proxy Authentication Required");
    REASONS.put(408, "Request Timeout");
    REASONS.put(409, "Conflict");
    REASONS.put(410, "Gone");
    REASONS.put(411, "Length Required");
    REASONS.put(412, "Precondition Failed");
    REASONS.put(413, "Request Entity Too Large");
    REASONS.put(414, "Request-URI Too Long");
    REASONS.put(415, "Unsupported Media Type");
    REASONS.put(416, "Requested Range Not Satisfiable");
    REASONS.put(417, "Expectation Failed");
    REASONS.put(500, "Internal Server Error");
    REASONS.put(501, "Not Implemented");
    REASONS.put(502, "Bad Gateway");
    REASONS.put(503, "Service Unavailable");
    REASONS.put(504, "Gateway Timeout");
    REASONS.put(505, "HTTP Version Not Supported");
  }

  private StatusReasons() {}

  /** The text a refusal of this status carries when it was built out of the status alone. */
  public static String message(int status) {
    final String reason = REASONS.get(status);
    return reason == null ? "HTTP " + status + " " : "HTTP " + status + " " + reason;
  }

  public static String reason(int status) {
    return REASONS.get(status);
  }
}
