package com.hubspot.singularity.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.hubspot.singularity.config.server.LoggingConfiguration;
import java.util.Map;
import org.slf4j.LoggerFactory;

/**
 * Applies what the configuration file says about logging: the level the root logger keeps and the level
 * each named logger keeps.
 */
public final class LoggingConfigurator {

  private LoggingConfigurator() {}

  public static void configure(LoggingConfiguration configuration) {
    if (configuration == null) {
      return;
    }
    if (!(LoggerFactory.getILoggerFactory() instanceof LoggerContext)) {
      return;
    }
    final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    if (configuration.getLevel() != null) {
      context.getLogger("ROOT").setLevel(Level.toLevel(configuration.getLevel()));
    }
    for (Map.Entry<String, String> logger : configuration.getLoggers().entrySet()) {
      context.getLogger(logger.getKey()).setLevel(Level.toLevel(logger.getValue()));
    }
  }
}
