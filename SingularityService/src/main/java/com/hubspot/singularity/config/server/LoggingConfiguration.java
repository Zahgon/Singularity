package com.hubspot.singularity.config.server;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The logging block of the configuration file: the level the root logger keeps and the levels named
 * loggers keep. The appenders a file may name are read and kept; what is written goes to the console.
 */
public class LoggingConfiguration {
  private String level = "INFO";
  private Map<String, String> loggers = Collections.emptyMap();
  private final Map<String, Object> other = new HashMap<>();

  @JsonProperty
  public String getLevel() {
    return level;
  }

  @JsonProperty
  public void setLevel(String level) {
    this.level = level;
  }

  @JsonProperty
  public Map<String, String> getLoggers() {
    return loggers;
  }

  @JsonProperty
  public void setLoggers(Map<String, String> loggers) {
    this.loggers = loggers;
  }

  @JsonAnySetter
  public void setOther(String name, Object value) {
    other.put(name, value);
  }

  public Map<String, Object> getOther() {
    return other;
  }
}
