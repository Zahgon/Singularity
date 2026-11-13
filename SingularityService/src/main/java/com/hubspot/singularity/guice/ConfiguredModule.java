package com.hubspot.singularity.guice;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.hubspot.singularity.config.SingularityConfiguration;

/**
 * A module that is given the configuration the service was started with before it binds anything.
 */
public abstract class ConfiguredModule implements Module {
  private SingularityConfiguration configuration;

  public void setConfiguration(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  protected SingularityConfiguration getConfiguration() {
    if (configuration == null) {
      throw new IllegalStateException("the module was not given a configuration");
    }
    return configuration;
  }

  @Override
  public abstract void configure(Binder binder);
}
