package com.hubspot.singularity.spring;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.server.ConnectorConfiguration;
import com.hubspot.singularity.config.server.ServerConfiguration;
import com.hubspot.singularity.jackson.SingularityObjectMappers;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * What the service is built out of before any of its application contexts exist: the configuration a
 * command line read, the object graph Guice built from it, the registry the service measures itself in, the
 * registry it reports its health in, and the object mappers it reads and writes with. Both the
 * application context and the administrative context are given this, so the two connectors answer out of
 * one service.
 */
public class SingularityPlatform {
  private final SingularityConfiguration configuration;
  private final MetricRegistry metricRegistry = new MetricRegistry();
  private final HealthCheckRegistry healthCheckRegistry = new HealthCheckRegistry();
  private final ObjectMapper objectMapper = SingularityObjectMappers.newObjectMapper();
  private final ObjectMapper apiObjectMapper = SingularityObjectMappers.newApiObjectMapper();
  private volatile Injector injector;

  public SingularityPlatform(SingularityConfiguration configuration) {
    this.configuration = configuration;
  }

  public SingularityConfiguration getConfiguration() {
    return configuration;
  }

  public ServerConfiguration getServerConfiguration() {
    return configuration.getServerConfiguration();
  }

  public MetricRegistry getMetricRegistry() {
    return metricRegistry;
  }

  public HealthCheckRegistry getHealthCheckRegistry() {
    return healthCheckRegistry;
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  /** The mapper the API reads and writes entities with. */
  public ObjectMapper getApiObjectMapper() {
    return apiObjectMapper;
  }

  public Injector getInjector() {
    return injector;
  }

  public void setInjector(Injector injector) {
    this.injector = injector;
  }

  /** Whether the configuration names a connector of its own for the administrative pages. */
  public boolean hasAdminConnector() {
    return getServerConfiguration().getAdminConnector() != null;
  }

  /** What the application context is given about the connector it answers the API on. */
  public Map<String, Object> applicationProperties() {
    final ConnectorConfiguration connector = getServerConfiguration()
      .getApplicationConnector();
    final Map<String, Object> properties = commonProperties();
    properties.put("server.port", connector == null ? 8080 : connector.getPort());
    if (connector != null && connector.getBindHost() != null) {
      properties.put("server.address", connector.getBindHost());
    }
    properties.put(
      "server.servlet.context-path",
      getServerConfiguration().applicationContext()
    );
    return properties;
  }

  /** What the administrative context is given about the connector it answers the pages on. */
  public Map<String, Object> adminProperties() {
    final ConnectorConfiguration connector = getServerConfiguration().getAdminConnector();
    final Map<String, Object> properties = commonProperties();
    properties.put("server.port", connector == null ? 8081 : connector.getPort());
    if (connector != null && connector.getBindHost() != null) {
      properties.put("server.address", connector.getBindHost());
    }
    properties.put("server.servlet.context-path", getServerConfiguration().adminContext());
    return properties;
  }

  private Map<String, Object> commonProperties() {
    final Map<String, Object> properties = new LinkedHashMap<>();
    // Nothing of the application's own is served from the class path, no page of the framework's own is
    // answered, and a target no resource claims reaches the service rather than a default page.
    properties.put("spring.web.resources.add-mappings", false);
    properties.put("spring.mvc.throw-exception-if-no-handler-found", true);
    properties.put("server.error.whitelabel.enabled", false);
    properties.put("spring.main.allow-bean-definition-overriding", true);
    properties.put("spring.jmx.enabled", false);
    properties.put("spring.mvc.pathmatch.matching-strategy", "ant_path_matcher");
    // The characters a target may carry: the connector reads what the service has always read, and leaves
    // what the path means to the service.
    properties.put("server.tomcat.relaxed-path-chars", "[,],^,`,{,|,},#,\\");
    properties.put("server.tomcat.relaxed-query-chars", "[,],^,`,{,|,},<,>,\\,\"");
    properties.put("server.servlet.encoding.enabled", false);
    // The description of the API is written on the path the API has always answered it on.
    properties.put("springdoc.api-docs.path", "/api/openapi.json");
    properties.put("springdoc.writer-with-order-by-keys", true);
    properties.put("server.tomcat.use-relative-redirects", false);
    // The connector writes no page of its own for a request it refuses before the service sees it.
    properties.put("server.tomcat.error-report-valve-enabled", false);
    return properties;
  }

  /** Hands the context the objects the service was built out of, before its own beans are created. */
  public void registerWith(ConfigurableApplicationContext context) {
    context.getBeanFactory().registerSingleton("singularityPlatform", this);
  }
}
