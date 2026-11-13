package com.hubspot.singularity.config;

/** A configuration file the service will not accept. */
public class ConfigurationException extends Exception {
  private static final long serialVersionUID = 1L;

  public ConfigurationException(String message) {
    super(message);
  }

  public ConfigurationException(String message, Throwable cause) {
    super(message, cause);
  }
}
