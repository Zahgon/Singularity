package com.hubspot.singularity.cli;

import com.hubspot.singularity.SingularityService;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * The command line of the service: it reads the arguments, writes the usage of the service and of each of
 * its commands, and hands a command the arguments it asked for. Running no command, or asking for help,
 * writes the usage and ends well; an argument the command line cannot read writes the usage after the
 * message that says what was wrong with it, and ends badly.
 */
public class Cli {
  private static final String COMMAND_NAME_ATTR = "command";
  private static final String[][] HELP = { {}, { "-h" }, { "--help" } };
  private static final String[][] VERSION = { { "-v" }, { "--version" } };

  private final PrintWriter stdOut;
  private final PrintWriter stdErr;
  private final Map<String, Command> commands;
  private final SingularityService<?> service;
  private final ArgumentParser parser;

  public Cli(
    JarLocation location,
    SingularityService<?> service,
    Iterable<Command> commands,
    OutputStream stdOut,
    OutputStream stdErr
  ) {
    this.stdOut =
      new PrintWriter(new OutputStreamWriter(stdOut, StandardCharsets.UTF_8), true);
    this.stdErr =
      new PrintWriter(new OutputStreamWriter(stdErr, StandardCharsets.UTF_8), true);
    this.commands = new LinkedHashMap<>();
    this.parser = buildParser(location);
    this.service = service;
    for (Command command : commands) {
      addCommand(command);
    }
  }

  /** Whether the arguments named something the service could do. */
  public boolean run(String... arguments) throws Exception {
    try {
      if (isFlag(HELP, arguments)) {
        parser.printHelp(stdOut);
      } else if (isFlag(VERSION, arguments)) {
        parser.printVersion(stdOut);
      } else {
        final Namespace namespace = parser.parseArgs(arguments);
        final Command command = commands.get(namespace.getString(COMMAND_NAME_ATTR));
        if (command == null) {
          throw new IllegalStateException("Command is not found");
        }
        try {
          command.run(service, namespace);
        } catch (Throwable e) {
          command.onError(this, namespace, e);
          return false;
        }
      }
      return true;
    } catch (HelpScreenException ignored) {
      return true;
    } catch (ArgumentParserException e) {
      stdErr.println(e.getMessage());
      e.getParser().printHelp(stdErr);
      return false;
    }
  }

  private static boolean isFlag(String[][] flags, String[] arguments) {
    for (String[] flag : flags) {
      if (Arrays.equals(arguments, flag)) {
        return true;
      }
    }
    return false;
  }

  private ArgumentParser buildParser(JarLocation location) {
    final String usage = "java -jar " + location;
    final ArgumentParser p = ArgumentParsers.newArgumentParser(usage, false);
    p.version(
      location
        .getVersion()
        .orElse(
          "No application version detected. Add a Implementation-Version " +
          "entry to your JAR's manifest to enable this."
        )
    );
    addHelp(p);
    p
      .addArgument("-v", "--version")
      .action(Arguments.help())
      .help("show the application version and exit");
    return p;
  }

  private void addHelp(ArgumentParser p) {
    p
      .addArgument("-h", "--help")
      .action(new SafeHelpAction(stdOut))
      .help("show this help message and exit")
      .setDefault(Arguments.SUPPRESS);
  }

  private void addCommand(Command command) {
    commands.put(command.getName(), command);
    parser.addSubparsers().help("available commands");
    final Subparser subparser = parser.addSubparsers().addParser(command.getName(), false);
    command.configure(subparser);
    addHelp(subparser);
    subparser
      .description(command.getDescription())
      .setDefault(COMMAND_NAME_ATTR, command.getName())
      .defaultHelp(true);
  }

  public PrintWriter getStdOut() {
    return stdOut;
  }

  public PrintWriter getStdErr() {
    return stdErr;
  }

  /** Writing the usage without ending the process, so that a command can still be given afterwards. */
  private static class SafeHelpAction implements ArgumentAction {
    private final PrintWriter out;

    SafeHelpAction(PrintWriter out) {
      this.out = out;
    }

    @Override
    public void run(
      ArgumentParser parser,
      Argument arg,
      Map<String, Object> attrs,
      String flag,
      Object value
    )
      throws ArgumentParserException {
      parser.printHelp(out);
      throw new HelpScreenException(parser);
    }

    @Override
    public void onAttach(Argument arg) {}

    @Override
    public boolean consumeArgument() {
      return false;
    }
  }
}
