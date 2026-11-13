package com.hubspot.singularity.config.server;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * One connector of the server block of the configuration file: the port the service listens on and the
 * host it binds to. The tuning keys a connector block may also carry are read and kept, so a configuration
 * file written for the service is still accepted whole, but they do not change what is answered.
 */
public class ConnectorConfiguration {
  private String type = "http";
  private int port = 8080;
  private String bindHost;
  private final Map<String, Object> other = new HashMap<>();

  @JsonProperty
  public String getType() {
    return type;
  }

  @JsonProperty
  public void setType(String type) {
    this.type = type;
  }

  @JsonProperty
  public int getPort() {
    return port;
  }

  @JsonProperty
  public void setPort(int port) {
    this.port = port;
  }

  @JsonProperty
  public String getBindHost() {
    return bindHost;
  }

  @JsonProperty
  public void setBindHost(String bindHost) {
    this.bindHost = bindHost;
  }

  @JsonAnySetter
  public void setOther(String name, Object value) {
    other.put(name, value);
  }

  public Map<String, Object> getOther() {
    return other;
  }

  public boolean isHttps() {
    return "https".equals(type);
  }
}
