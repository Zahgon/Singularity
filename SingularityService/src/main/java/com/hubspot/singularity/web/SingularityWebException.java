package com.hubspot.singularity.web;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A refusal the service itself decided on: the status it answers with, the text the answer carries, and the
 * text the service wrote about it.
 *
 * The two texts are not always the same. A refusal the service builds out of a status alone carries the
 * status and its reason as its text, whatever the service said about it; a refusal the service builds out of
 * a message carries that message. The API has always answered them that way, and so does this.
 */
public class SingularityWebException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private final int status;
  private final String detail;
  private final Map<String, String> headers = new LinkedHashMap<>();

  public SingularityWebException(int status, String message, String detail) {
    super(message);
    this.status = status;
    this.detail = detail;
  }

  public SingularityWebException(int status, String message) {
    this(status, message, message);
  }

  public int getStatus() {
    return status;
  }

  /** What the service said about the refusal, which is not always what the answer carries. */
  public String getDetail() {
    return detail;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public SingularityWebException header(String name, String value) {
    headers.put(name, value);
    return this;
  }
}
