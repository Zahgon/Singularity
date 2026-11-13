package com.hubspot.singularity.cli;

import com.hubspot.singularity.SingularityService;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.server.DataSourceConfiguration;
import java.sql.Connection;
import java.util.Optional;
import javax.sql.DataSource;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * Runs the migrations of the history database the configuration file names.
 */
public class DbCommand extends ConfiguredCommand {
  private static final String MIGRATIONS_FILE = "migrations.xml";

  public DbCommand() {
    super("db", "Run database migration tasks");
  }

  @Override
  public void configure(Subparser subparser) {
    subparser
      .addArgument("subcommand")
      .nargs("?")
      .setDefault("migrate")
      .help("the migration task to run: migrate, status or tag");
    subparser.addArgument("--migrations").help("the migrations file to run");
    addFileArgument(subparser);
  }

  @Override
  protected void run(
    SingularityService<?> service,
    Namespace namespace,
    SingularityConfiguration configuration
  )
    throws Exception {
    final Optional<DataSourceConfiguration> database = configuration.getDatabaseConfiguration();
    if (!database.isPresent()) {
      throw new IllegalStateException(
        "The configuration file names no database to migrate"
      );
    }
    final DataSource dataSource = database.get().build("migrations");
    final String migrations = Optional
      .ofNullable(namespace.getString("migrations"))
      .orElse(MIGRATIONS_FILE);
    try (Connection connection = dataSource.getConnection()) {
      final Database target = DatabaseFactory
        .getInstance()
        .findCorrectDatabaseImplementation(new JdbcConnection(connection));
      final Liquibase liquibase = new Liquibase(
        migrations,
        new ClassLoaderResourceAccessor(),
        target
      );
      final String subcommand = namespace.getString("subcommand");
      if ("status".equals(subcommand)) {
        liquibase.reportStatus(true, "", new java.io.PrintWriter(System.out, true));
      } else {
        liquibase.update("");
      }
    }
  }
}
