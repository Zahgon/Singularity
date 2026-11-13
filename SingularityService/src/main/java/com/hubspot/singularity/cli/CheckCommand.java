package com.hubspot.singularity.cli;

import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.config.SingularityConfiguration;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the configuration file and says nothing further when the service could run on it.
 */
public class CheckCommand extends ConfiguredCommand {
  private static final Logger LOG = LoggerFactory.getLogger(CheckCommand.class);

  public CheckCommand() {
    super("check", "Parses and validates the configuration file");
  }

  @Override
  protected void run(
    SingularityService<?> service,
    Namespace namespace,
    SingularityConfiguration configuration
  ) {
    LOG.info("Configuration is OK");
  }

  /** The message alone: the file's own error already says where in the file the error is. */
  @Override
  public void onError(Cli cli, Namespace namespace, Throwable e) {
    cli.getStdErr().println(e.getMessage());
  }
}
