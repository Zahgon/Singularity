package com.hubspot.singularity.cli;

import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.config.SingularityConfiguration;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 * Runs the service as an HTTP server on the connectors its configuration file names.
 */
public class ServerCommand extends ConfiguredCommand {

  public ServerCommand() {
    super("server", "Runs Singularity as an HTTP server");
  }

  @Override
  protected void run(
    SingularityService<?> service,
    Namespace namespace,
    SingularityConfiguration configuration
  )
    throws Exception {
    service.startServer(configuration);
  }
}
