package com.hubspot.singularity.config.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import jakarta.validation.Valid;

/**
 * The blocks of the configuration file that belong to the service itself rather than to Singularity: the
 * server it runs, what it logs and what it measures. Singularity's own configuration extends this, so a
 * configuration file keeps the shape it has always had.
 */
public class ServiceConfiguration {
  @Valid
  private ServerConfiguration server = new ServerConfiguration();

  @Valid
  private LoggingConfiguration logging = new LoggingConfiguration();

  private final Map<String, Object> metrics = new HashMap<>();

  @JsonProperty("server")
  public ServerConfiguration getServerConfiguration() {
    return server;
  }

  @JsonProperty("server")
  public void setServerConfiguration(ServerConfiguration server) {
    this.server = server;
  }

  @JsonProperty("logging")
  public LoggingConfiguration getLoggingConfiguration() {
    return logging;
  }

  @JsonProperty("logging")
  public void setLoggingConfiguration(LoggingConfiguration logging) {
    this.logging = logging;
  }

  @JsonProperty("metrics")
  public Map<String, Object> getMetricsConfiguration() {
    return metrics;
  }

  @JsonProperty("metrics")
  public void setMetricsConfiguration(Map<String, Object> values) {
    metrics.clear();
    if (values != null) {
      metrics.putAll(values);
    }
  }
}
