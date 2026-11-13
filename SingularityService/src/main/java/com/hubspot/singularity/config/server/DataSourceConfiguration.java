package com.hubspot.singularity.config.server;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;

/**
 * The database block of the configuration file: the driver, the url and the credentials of the history
 * store, with the pool keys such a block may carry read and kept. The block keeps the shape the service has
 * always read it in.
 */
public class DataSourceConfiguration {
  private String driverClass;
  private String url;
  private String user;
  private String password;
  private Map<String, String> properties = Collections.emptyMap();
  private int minSize = 10;
  private int maxSize = 100;
  private String validationQuery = "/* Health Check */ SELECT 1";
  private final Map<String, Object> other = new HashMap<>();

  @JsonProperty
  public String getDriverClass() {
    return driverClass;
  }

  @JsonProperty
  public void setDriverClass(String driverClass) {
    this.driverClass = driverClass;
  }

  @JsonProperty
  public String getUrl() {
    return url;
  }

  @JsonProperty
  public void setUrl(String url) {
    this.url = url;
  }

  @JsonProperty
  public String getUser() {
    return user;
  }

  @JsonProperty
  public void setUser(String user) {
    this.user = user;
  }

  @JsonProperty
  public String getPassword() {
    return password;
  }

  @JsonProperty
  public void setPassword(String password) {
    this.password = password;
  }

  @JsonProperty
  public Map<String, String> getProperties() {
    return properties;
  }

  @JsonProperty
  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  @JsonProperty
  public int getMinSize() {
    return minSize;
  }

  @JsonProperty
  public void setMinSize(int minSize) {
    this.minSize = minSize;
  }

  @JsonProperty
  public int getMaxSize() {
    return maxSize;
  }

  @JsonProperty
  public void setMaxSize(int maxSize) {
    this.maxSize = maxSize;
  }

  @JsonProperty
  public String getValidationQuery() {
    return validationQuery;
  }

  @JsonProperty
  public void setValidationQuery(String validationQuery) {
    this.validationQuery = validationQuery;
  }

  @JsonAnySetter
  public void setOther(String name, Object value) {
    other.put(name, value);
  }

  public Map<String, Object> getOther() {
    return other;
  }

  /** The pooled data source this block describes. */
  public DataSource build(String poolName) {
    HikariConfig config = new HikariConfig();
    if (driverClass != null) {
      config.setDriverClassName(driverClass);
    }
    config.setJdbcUrl(url);
    config.setUsername(user);
    config.setPassword(password);
    config.setPoolName(poolName);
    config.setMinimumIdle(minSize);
    config.setMaximumPoolSize(Math.max(maxSize, minSize));
    config.setConnectionTestQuery(validationQuery);
    for (Map.Entry<String, String> property : properties.entrySet()) {
      config.addDataSourceProperty(property.getKey(), property.getValue());
    }
    return new HikariDataSource(config);
  }
}
