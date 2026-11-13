package com.hubspot.singularity;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Module;
import com.hubspot.singularity.cli.Command;
import com.hubspot.singularity.config.SingularityConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Example on how to embed Singularity into another piece of code or how to extend Singularity with additional
 * functionality that is not in the service out-of-the-box.
 *
 * The {@link SingularityService} class provides a number of extension points to hook additional functionality
 * into the service and by building a separate artifact, it is possible to package the code differently or add
 * additional runtime dependencies without having to modify the main SingularityService build itself.
 */
public class EmbeddedSingularityExample
  extends SingularityService<SingularityConfiguration> {

  @Override
  public Iterable<? extends Module> getGuiceModules() {
    return ImmutableSet.of();
  }

  @Override
  public Iterable<? extends Command> getAdditionalCommands() {
    return ImmutableSet.of();
  }

  @Override
  public void initialize() {
    super.initialize();
  }

  @Override
  public void modifyApplication(final SpringApplicationBuilder builder) {
    super.modifyApplication(builder);
  }

  public static void main(final String[] args) throws Exception {
    new EmbeddedSingularityExample().run(args);
  }
}
