package com.hubspot.singularity.web;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * What an entity the service will not accept carries: the constraints it breaks, one to an entry.
 */
public class ValidationErrorMessage {
  private final List<String> errors;

  @JsonCreator
  public ValidationErrorMessage(@JsonProperty("errors") List<String> errors) {
    this.errors = errors;
  }

  @JsonProperty("errors")
  public List<String> getErrors() {
    return errors;
  }
}
