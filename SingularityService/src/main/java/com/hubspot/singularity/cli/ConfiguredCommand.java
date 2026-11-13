package com.hubspot.singularity.cli;

import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.config.SingularityConfiguration;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * A command whose first argument is the configuration file it runs on. The file is read and checked before
 * the command is run, and what the file says about logging is applied before anything else happens.
 */
public abstract class ConfiguredCommand extends Command {

  protected ConfiguredCommand(String name, String description) {
    super(name, description);
  }

  @Override
  public void configure(Subparser subparser) {
    addFileArgument(subparser);
  }

  protected Argument addFileArgument(Subparser subparser) {
    return subparser
      .addArgument("file")
      .nargs("?")
      .help("application configuration file");
  }

  @Override
  public void run(SingularityService<?> service, Namespace namespace) throws Exception {
    final SingularityConfiguration configuration = service.parseConfiguration(
      namespace.getString("file")
    );
    service.configureLogging(configuration);
    run(service, namespace, configuration);
  }

  protected abstract void run(
    SingularityService<?> service,
    Namespace namespace,
    SingularityConfiguration configuration
  )
    throws Exception;
}
