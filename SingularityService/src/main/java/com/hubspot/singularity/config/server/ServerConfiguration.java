package com.hubspot.singularity.config.server;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The server block of the configuration file: the connectors the service answers on, the context path each
 * of them serves under, the mapping the API is reached through, and the methods a request may carry. The
 * block is read in both of the shapes the service has always accepted, the one with connector lists and the
 * one with a single connector, and the tuning keys it may also carry are read and kept.
 */
public class ServerConfiguration {
  public static final String DEFAULT = "default";
  public static final String SIMPLE = "simple";

  private String type = DEFAULT;
  private String applicationContextPath = "/";
  private String adminContextPath = "/";
  private String rootPath = "/*";
  private List<ConnectorConfiguration> applicationConnectors = new ArrayList<>(
    Arrays.asList(connector(8080))
  );
  private List<ConnectorConfiguration> adminConnectors = new ArrayList<>(
    Arrays.asList(connector(8081))
  );
  private ConnectorConfiguration connector = connector(8080);
  private GzipConfiguration gzip = new GzipConfiguration();
  private Set<String> allowedMethods = new LinkedHashSet<>(
    Arrays.asList("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH")
  );
  private final Map<String, Object> other = new HashMap<>();

  private static ConnectorConfiguration connector(int port) {
    ConnectorConfiguration c = new ConnectorConfiguration();
    c.setPort(port);
    return c;
  }

  @JsonProperty
  public String getType() {
    return type;
  }

  @JsonProperty
  public void setType(String type) {
    this.type = type;
  }

  public boolean isSimple() {
    return SIMPLE.equals(type);
  }

  @JsonProperty
  public String getApplicationContextPath() {
    return applicationContextPath;
  }

  @JsonProperty
  public void setApplicationContextPath(String applicationContextPath) {
    this.applicationContextPath = applicationContextPath;
  }

  @JsonProperty
  public String getAdminContextPath() {
    return adminContextPath;
  }

  @JsonProperty
  public void setAdminContextPath(String adminContextPath) {
    this.adminContextPath = adminContextPath;
  }

  @JsonProperty
  public String getRootPath() {
    return rootPath;
  }

  @JsonProperty
  public void setRootPath(String rootPath) {
    this.rootPath = rootPath;
  }

  @JsonProperty
  public List<ConnectorConfiguration> getApplicationConnectors() {
    return applicationConnectors;
  }

  @JsonProperty
  public void setApplicationConnectors(List<ConnectorConfiguration> applicationConnectors) {
    this.applicationConnectors = applicationConnectors;
  }

  @JsonProperty
  public List<ConnectorConfiguration> getAdminConnectors() {
    return adminConnectors;
  }

  @JsonProperty
  public void setAdminConnectors(List<ConnectorConfiguration> adminConnectors) {
    this.adminConnectors = adminConnectors;
  }

  @JsonProperty
  public ConnectorConfiguration getConnector() {
    return connector;
  }

  @JsonProperty
  public void setConnector(ConnectorConfiguration connector) {
    this.connector = connector;
  }

  @JsonProperty
  public GzipConfiguration getGzip() {
    return gzip;
  }

  @JsonProperty
  public void setGzip(GzipConfiguration gzip) {
    this.gzip = gzip;
  }

  @JsonProperty
  public Set<String> getAllowedMethods() {
    return allowedMethods;
  }

  @JsonProperty
  public void setAllowedMethods(Set<String> allowedMethods) {
    this.allowedMethods = allowedMethods;
  }

  @JsonAnySetter
  public void setOther(String name, Object value) {
    other.put(name, value);
  }

  public Map<String, Object> getOther() {
    return other;
  }

  /** The connector the API is answered on: the single one of a simple block, else the first plain http one. */
  public ConnectorConfiguration getApplicationConnector() {
    if (isSimple()) {
      return connector;
    }
    for (ConnectorConfiguration candidate : applicationConnectors) {
      if (!candidate.isHttps()) {
        return candidate;
      }
    }
    return applicationConnectors.isEmpty() ? null : applicationConnectors.get(0);
  }

  /**
   * The connector the administrative pages are answered on, or nothing when the block is the simple one,
   * which serves them on the application connector.
   */
  public ConnectorConfiguration getAdminConnector() {
    if (isSimple()) {
      return null;
    }
    for (ConnectorConfiguration candidate : adminConnectors) {
      if (!candidate.isHttps()) {
        return candidate;
      }
    }
    return adminConnectors.isEmpty() ? null : adminConnectors.get(0);
  }

  /** The context path the API is served under, as a path that starts with a slash and does not end in one. */
  public String applicationContext() {
    return normalise(applicationContextPath);
  }

  /** The context path the administrative pages are served under. */
  public String adminContext() {
    return normalise(adminContextPath);
  }

  private static String normalise(String path) {
    if (path == null || path.isEmpty() || "/".equals(path)) {
      return "";
    }
    String value = path.startsWith("/") ? path : "/" + path;
    while (value.endsWith("/")) {
      value = value.substring(0, value.length() - 1);
    }
    return value;
  }
}
