package com.hubspot.singularity.guice;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.singularity.spring.SingularityPlatform;
import javax.inject.Inject;
import javax.inject.Provider;

/**
 * The object mapper the service's own objects read and write with: a copy of the one the API reads and
 * writes entities with, so that a change one of them makes is not a change the other sees.
 */
public class SingularityObjectMapperProvider implements Provider<ObjectMapper> {
  private final SingularityPlatform platform;

  @Inject
  public SingularityObjectMapperProvider(final SingularityPlatform platform) {
    this.platform = checkNotNull(platform, "platform is null");
  }

  @Override
  public ObjectMapper get() {
    return platform.getObjectMapper().copy();
  }
}
