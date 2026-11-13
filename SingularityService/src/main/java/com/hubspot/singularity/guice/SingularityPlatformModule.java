package com.hubspot.singularity.guice;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.config.server.ServerConfiguration;
import com.hubspot.singularity.spring.SingularityPlatform;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * What the object graph is given of the service the configuration describes: the configuration itself, the
 * registries the service measures and reports its health in, and the request being answered, when one is.
 */
public class SingularityPlatformModule extends AbstractModule {
  private final SingularityPlatform platform;

  public SingularityPlatformModule(SingularityPlatform platform) {
    this.platform = platform;
  }

  @Override
  protected void configure() {
    bind(SingularityPlatform.class).toInstance(platform);
    bind(SingularityConfiguration.class).toInstance(platform.getConfiguration());
    bind(ServerConfiguration.class).toInstance(platform.getServerConfiguration());
    bind(MetricRegistry.class).toInstance(platform.getMetricRegistry());
    bind(HealthCheckRegistry.class).toInstance(platform.getHealthCheckRegistry());
  }

  /** The request being answered on this thread, when the thread is answering one. */
  @Provides
  public HttpServletRequest providesCurrentHttpRequest() {
    if (
      RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes
    ) {
      return (
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes()
      ).getRequest();
    }
    throw new IllegalStateException("no request is being answered on this thread");
  }
}
