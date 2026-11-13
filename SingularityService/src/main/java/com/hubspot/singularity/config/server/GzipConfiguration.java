package com.hubspot.singularity.config.server;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/**
 * The gzip block of the server block: whether answers are compressed and from which size on.
 */
public class GzipConfiguration {
  private boolean enabled = true;
  private String minimumEntitySize = "256 bytes";
  private final Map<String, Object> other = new HashMap<>();

  @JsonProperty
  public boolean isEnabled() {
    return enabled;
  }

  @JsonProperty
  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @JsonProperty
  public String getMinimumEntitySize() {
    return minimumEntitySize;
  }

  @JsonProperty
  public void setMinimumEntitySize(String minimumEntitySize) {
    this.minimumEntitySize = minimumEntitySize;
  }

  /** The size in bytes a compressed answer must reach, read from the size the file names. */
  public int getMinimumEntitySizeBytes() {
    return (int) DataSize.parse(minimumEntitySize);
  }

  @JsonAnySetter
  public void setOther(String name, Object value) {
    other.put(name, value);
  }

  public Map<String, Object> getOther() {
    return other;
  }
}
