package com.hubspot.singularity.cli;

import com.hubspot.singularity.SingularityService;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * One command of the service's command line.
 */
public abstract class Command {
  private final String name;
  private final String description;

  protected Command(String name, String description) {
    this.name = name;
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  /** The arguments this command reads, added to the parser the command line gives it. */
  public abstract void configure(Subparser subparser);

  public abstract void run(SingularityService<?> service, Namespace namespace)
    throws Exception;

  /**
   * What is written when the command cannot run: the stack trace, which is what a command that does not
   * know better says about its own failure.
   */
  public void onError(Cli cli, Namespace namespace, Throwable e) {
    e.printStackTrace(cli.getStdErr());
  }
}
