package com.hubspot.singularity.spring;

import com.hubspot.singularity.config.server.GzipConfiguration;
import com.hubspot.singularity.web.ContainerErrorValve;
import com.hubspot.singularity.web.ResponseCompressionFilter;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.context.annotation.Import;

/**
 * The context that answers the administrative connector: the pages the service is watched through and the
 * tasks it is told to run, on a connector of their own, as the configuration file asks for them.
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
@Import({ AdminController.class, AdminErrorController.class })
public class SingularityAdminApplication {

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

  /** The pages say how long they are and are compressed, as the connector has always answered them. */
  @Bean
  public FilterRegistrationBean<ResponseCompressionFilter> responseCompressionFilter(
    SingularityPlatform platform
  ) {
    final GzipConfiguration gzip = platform.getServerConfiguration().getGzip();
    final FilterRegistrationBean<ResponseCompressionFilter> registration = new FilterRegistrationBean<>(
      new ResponseCompressionFilter(
        gzip.isEnabled() ? gzip.getMinimumEntitySizeBytes() : Integer.MAX_VALUE,
        java.util.List.of("GET")
      )
    );
    registration.addUrlPatterns("/*");
    registration.setDispatcherTypes(EnumSet.of(DispatcherType.REQUEST, DispatcherType.ERROR));
    registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
    return registration;
  }
}
