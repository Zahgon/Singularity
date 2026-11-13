package com.hubspot.singularity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.Stage;
import com.hubspot.singularity.auth.SingularityAuthModule;
import com.hubspot.singularity.cli.CheckCommand;
import com.hubspot.singularity.cli.Cli;
import com.hubspot.singularity.cli.Command;
import com.hubspot.singularity.cli.DbCommand;
import com.hubspot.singularity.cli.JarLocation;
import com.hubspot.singularity.cli.ServerCommand;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.ConfigurationException;
import com.hubspot.singularity.config.ConfigurationFactory;
import com.hubspot.singularity.config.ConfigurationSourceProvider;
import com.hubspot.singularity.config.FileConfigurationSourceProvider;
import com.hubspot.singularity.config.MergingSourceProvider;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.guice.ConfiguredModule;
import com.hubspot.singularity.guice.SingularityObjectMapperProvider;
import com.hubspot.singularity.guice.SingularityPlatformModule;
import com.hubspot.singularity.jackson.SingularityObjectMappers;
import com.hubspot.singularity.logging.LoggingConfigurator;
import com.hubspot.singularity.spring.SingularityAdminApplication;
import com.hubspot.singularity.spring.SingularityPlatform;
import com.hubspot.singularity.spring.SingularityWebApplication;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Singularity: the service, its command line and the extension points something that embeds it hooks into.
 *
 * The service is a Spring Boot application. The command line reads a YAML configuration file, the
 * configuration decides the connectors and the context paths, and the object graph of the service itself is
 * built by Guice and handed to the application context, so the resources, managers, datastores and
 * schedulers keep the bindings they have always had.
 */
public class SingularityService<T extends SingularityConfiguration> {
  private static final String SINGULARITY_DEFAULT_CONFIGURATION_PROPERTY =
    "singularityDefaultConfiguration";

  public static final String API_BASE_PATH = ApiPaths.API_BASE_PATH;

  private final ObjectMapper configurationObjectMapper = SingularityObjectMappers.newYamlObjectMapper();
  private ConfigurationSourceProvider configurationSourceProvider = new FileConfigurationSourceProvider();

  /**
   * Called before anything is read or started. A file of defaults named by the
   * {@code singularityDefaultConfiguration} property is merged under every configuration file that is read.
   */
  public void initialize() {
    final String defaults = System.getProperty(
      SINGULARITY_DEFAULT_CONFIGURATION_PROPERTY
    );
    if (!Strings.isNullOrEmpty(defaults)) {
      configurationSourceProvider =
        new MergingSourceProvider(
          configurationSourceProvider,
          defaults,
          configurationObjectMapper,
          new YAMLFactory()
        );
    }
  }

  @SuppressWarnings("unchecked")
  public Class<T> getConfigurationClass() {
    return (Class<T>) SingularityConfiguration.class;
  }

  public ConfigurationSourceProvider getConfigurationSourceProvider() {
    return configurationSourceProvider;
  }

  public void setConfigurationSourceProvider(ConfigurationSourceProvider provider) {
    this.configurationSourceProvider = provider;
  }

  /** The configuration the file at this path holds, or the default configuration when there is no path. */
  public T parseConfiguration(String path) throws IOException, ConfigurationException {
    final ConfigurationFactory<T> factory = new ConfigurationFactory<>(
      getConfigurationClass(),
      configurationObjectMapper
    );
    return path == null
      ? factory.build()
      : factory.build(configurationSourceProvider, path);
  }

  public void configureLogging(SingularityConfiguration configuration) {
    LoggingConfigurator.configure(configuration.getLoggingConfiguration());
  }

  /** The commands the command line reads, in the order its usage lists them. */
  public Iterable<Command> getCommands() {
    final List<Command> commands = new ArrayList<>();
    commands.add(new ServerCommand());
    commands.add(new CheckCommand());
    commands.add(new DbCommand());
    for (Command command : getAdditionalCommands()) {
      commands.add(command);
    }
    return commands;
  }

  /**
   * Commands in addition to the ones the service brings. This is an extension point when embedding
   * Singularity into a custom service.
   */
  public Iterable<? extends Command> getAdditionalCommands() {
    return ImmutableList.of();
  }

  /** The module that binds the service itself. */
  public Module getServiceModule() {
    return new SingularityServiceModule();
  }

  /** The module that decides which object mapper the API reads and writes entities with. */
  public Module getObjectMapperModule() {
    return binder ->
      binder
        .bind(ObjectMapper.class)
        .toProvider(SingularityObjectMapperProvider.class)
        .in(Scopes.SINGLETON);
  }

  public Module getAuthModule() {
    return new SingularityAuthModule();
  }

  /**
   * Guice modules used in addition to the modules required by Singularity. This is an extension point when
   * embedding Singularity into a custom service.
   */
  public Iterable<? extends Module> getGuiceModules() {
    return ImmutableList.of();
  }

  public Stage getGuiceStage() {
    return Stage.PRODUCTION;
  }

  /**
   * A chance to change the application before it is started. This is an extension point when embedding
   * Singularity into a custom service.
   */
  public void modifyApplication(SpringApplicationBuilder builder) {}

  /**
   * What the connector is told before it is built: an escaped separator in a target is a character of the
   * value it stands in, and a backslash is a character like any other, which is how the service has always
   * read a target.
   */
  private static void allowTargetCharacters() {
    System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
    System.setProperty("org.apache.catalina.connector.CoyoteAdapter.ALLOW_BACKSLASH", "true");
  }

  /** Builds the object graph of the service and starts the connectors its configuration names. */
  public ConfigurableApplicationContext startServer(SingularityConfiguration configuration)
    throws Exception {
    allowTargetCharacters();
    final SingularityPlatform platform = new SingularityPlatform(configuration);
    final List<Module> modules = new ArrayList<>();
    modules.add(new SingularityPlatformModule(platform));
    modules.add(getServiceModule());
    modules.add(getObjectMapperModule());
    modules.add(getAuthModule());
    for (Module module : getGuiceModules()) {
      modules.add(module);
    }
    // A module that is written against the configuration is given it before it binds anything.
    for (Module module : modules) {
      if (module instanceof ConfiguredModule) {
        ((ConfiguredModule) module).setConfiguration(configuration);
      }
    }
    final Injector injector = Guice.createInjector(getGuiceStage(), modules);
    platform.setInjector(injector);

    final SpringApplicationBuilder builder = new SpringApplicationBuilder(
      SingularityWebApplication.class
    )
      .bannerMode(Banner.Mode.OFF)
      .logStartupInfo(false)
      .registerShutdownHook(true)
      .properties(platform.applicationProperties())
      .initializers(platform::registerWith);
    modifyApplication(builder);
    final ConfigurableApplicationContext context = builder.run();

    if (platform.hasAdminConnector()) {
      new SpringApplicationBuilder(SingularityAdminApplication.class)
        .bannerMode(Banner.Mode.OFF)
        .logStartupInfo(false)
        .registerShutdownHook(false)
        .properties(platform.adminProperties())
        .initializers(platform::registerWith)
        .run();
    }
    return context;
  }

  /** Reads the arguments and does what they name, ending badly only when a command could not be run. */
  public void run(String... arguments) throws Exception {
    initialize();
    final Cli cli = new Cli(
      new JarLocation(getClass()),
      this,
      getCommands(),
      System.out,
      System.err
    );
    if (!cli.run(arguments)) {
      onFatalError();
    }
  }

  protected void onFatalError() {
    System.exit(1);
  }

  public static void main(final String[] args) throws Exception {
    try {
      new SingularityService<SingularityConfiguration>().run(args);
    } catch (final Throwable t) {
      t.printStackTrace();
      System.exit(1);
    }
  }
}
