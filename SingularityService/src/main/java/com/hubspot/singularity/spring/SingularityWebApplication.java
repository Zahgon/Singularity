package com.hubspot.singularity.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Injector;
import com.hubspot.singularity.ServerProvider;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.server.GzipConfiguration;
import com.hubspot.singularity.web.AllowedMethods;
import com.hubspot.singularity.web.AllowedMethodsFilter;
import com.hubspot.singularity.web.OptionsFilter;
import com.hubspot.singularity.web.BlankEntityFilter;
import com.hubspot.singularity.web.ContainerErrorValve;
import com.hubspot.singularity.web.CrossOriginFilter;
import com.hubspot.singularity.web.MediaTypeFilter;
import com.hubspot.singularity.web.RequestDecompressionFilter;
import com.hubspot.singularity.web.ResponseCompressionFilter;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import java.util.List;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * The application context that answers the API: the connector, the dispatcher and the resources, with the
 * parts of the framework the service does not use left out.
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration(
  exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    JdbcTemplateAutoConfiguration.class,
    JdbcClientAutoConfiguration.class,
    SqlInitializationAutoConfiguration.class,
    TransactionAutoConfiguration.class,
    JacksonAutoConfiguration.class,
    GsonAutoConfiguration.class
  }
)
@Import(
  {
    SingularityMvcConfiguration.class,
    SingularityResourceRegistrar.class,
    SingularityExceptionHandler.class,
    ApplicationErrorController.class
  }
)
public class SingularityWebApplication {

  @Bean
  public SingularityConfiguration singularityConfiguration(SingularityPlatform platform) {
    return platform.getConfiguration();
  }

  @Bean
  public Injector singularityInjector(SingularityPlatform platform) {
    return platform.getInjector();
  }

  @Bean
  public ObjectMapper objectMapper(SingularityPlatform platform) {
    return platform.getObjectMapper();
  }

  @Bean
  public ManagedLifecycle managedLifecycle(SingularityPlatform platform) {
    return new ManagedLifecycle(platform.getInjector());
  }

  /** How long an answer is, and whether it is compressed. */
  @Bean
  public FilterRegistrationBean<ResponseCompressionFilter> responseCompressionFilter(
    SingularityPlatform platform
  ) {
    final GzipConfiguration gzip = platform.getServerConfiguration().getGzip();
    final FilterRegistrationBean<ResponseCompressionFilter> registration = new FilterRegistrationBean<>(
      new ResponseCompressionFilter(
        gzip.isEnabled() ? gzip.getMinimumEntitySizeBytes() : Integer.MAX_VALUE,
        List.of("GET")
      )
    );
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(
      EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR)
    );
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 2);
    return registration;
  }

  /** An entity the request says is compressed is read as the coding it names. */
  @Bean
  public FilterRegistrationBean<RequestDecompressionFilter> requestDecompressionFilter() {
    final FilterRegistrationBean<RequestDecompressionFilter> registration = new FilterRegistrationBean<>(
      new RequestDecompressionFilter()
    );
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 3);
    return registration;
  }

  /** A method the service does not answer at all is refused before anything of the service sees it. */
  @Bean
  public FilterRegistrationBean<AllowedMethodsFilter> allowedMethodsFilter(
    SingularityPlatform platform
  ) {
    final FilterRegistrationBean<AllowedMethodsFilter> registration = new FilterRegistrationBean<>(
      new AllowedMethodsFilter(platform.getServerConfiguration().getAllowedMethods())
    );
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    return registration;
  }

/** The connector writes its own refusals the way the service has always written them. */
  @Bean
  public org.springframework.boot.web.server.WebServerFactoryCustomizer<org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory> containerErrors() {
    return factory ->
      factory.addContextCustomizers(context -> {
        if (context.getParent() instanceof org.apache.catalina.Host) {
          ((org.apache.catalina.core.StandardHost) context.getParent()).setErrorReportValveClass(
              ContainerErrorValve.class.getName()
            );
        }
      });
  }

  /** An entity that names a kind that is not written as one is refused by the connector. */
  @Bean
  public FilterRegistrationBean<MediaTypeFilter> mediaTypeFilter() {
    final FilterRegistrationBean<MediaTypeFilter> registration = new FilterRegistrationBean<>(
      new MediaTypeFilter()
    );
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
    return registration;
  }

  /** An entity of nothing but blanks is no entity at all. */
  @Bean
  public FilterRegistrationBean<BlankEntityFilter> blankEntityFilter() {
    final FilterRegistrationBean<BlankEntityFilter> registration = new FilterRegistrationBean<>(
      new BlankEntityFilter()
    );
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 6);
    return registration;
  }

  /** A request that asks what a target allows is answered by the service itself. */
  @Bean
  public FilterRegistrationBean<OptionsFilter> optionsFilter(AllowedMethods allowed) {
    final FilterRegistrationBean<OptionsFilter> registration = new FilterRegistrationBean<>(
      new OptionsFilter(allowed)
    );
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 5);
    return registration;
  }

  /** The cross-origin headers, when the configuration file asks for them. */
  @Bean
  public FilterRegistrationBean<CrossOriginFilter> crossOriginFilter(
    SingularityPlatform platform
  ) {
    final SingularityConfiguration configuration = platform.getConfiguration();
    final FilterRegistrationBean<CrossOriginFilter> registration = new FilterRegistrationBean<>(
      new CrossOriginFilter(configuration.getCors())
    );
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 4);
    registration.setEnabled(
      configuration.isEnableCorsFilter() || configuration.getCors().isEnabled()
    );
    return registration;
  }

  /** Once the service answers requests, what stops it answering them is known. */
  @Bean
  public ApplicationListener<ApplicationReadyEvent> serverStartedListener(
    SingularityPlatform platform
  ) {
    return event -> {
      final ServerProvider provider = platform
        .getInjector()
        .getInstance(ServerProvider.class);
      provider.serverStarted(() -> event.getApplicationContext().close());
    };
  }
}
