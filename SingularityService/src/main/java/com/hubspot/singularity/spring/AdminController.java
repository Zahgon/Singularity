package com.hubspot.singularity.spring;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.codahale.metrics.json.HealthCheckModule;
import com.codahale.metrics.json.MetricsModule;
import com.codahale.metrics.jvm.ThreadDump;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binding;
import com.google.inject.Key;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * The administrative pages of the service: the menu they are reached through, whether the service answers at
 * all, what its threads are doing, whether it is healthy, what it measures of itself, and the tasks it can
 * be told to run.
 */
@Controller
public class AdminController {
  private static final String PLAIN_TEXT = "text/plain;charset=iso-8859-1";
  /** What a task writes about what it did, which the connector writes in the wider encoding. */
  private static final String TASK_TEXT = "text/plain;charset=utf-8";
  private static final String NO_CACHE = "must-revalidate,no-cache,no-store";
  private static final String MENU =
    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\"\n" +
    "        \"http://www.w3.org/TR/html4/loose.dtd\">\n" +
    "<html>\n" +
    "<head>\n" +
    "  <title>Metrics</title>\n" +
    "</head>\n" +
    "<body>\n" +
    "  <h1>Operational Menu</h1>\n" +
    "  <ul>\n" +
    "    <li><a href=\"%1$s/metrics?pretty=true\">Metrics</a></li>\n" +
    "    <li><a href=\"%1$s/ping\">Ping</a></li>\n" +
    "    <li><a href=\"%1$s/threads\">Threads</a></li>\n" +
    "    <li><a href=\"%1$s/healthcheck?pretty=true\">Healthcheck</a></li>\n" +
    "    <li><a href=\"%1$s/pprof\">CPU Profile</a></li>\n" +
    "    <li><a href=\"%1$s/pprof?state=blocked\">CPU Contention</a></li>\n" +
    "  </ul>\n" +
    "</body>\n" +
    "</html>\n";

  private final SingularityPlatform platform;
  private final ObjectMapper healthCheckMapper = new ObjectMapper()
    .registerModule(new HealthCheckModule());
  private final ObjectMapper metricsMapper = new ObjectMapper()
    .registerModule(new MetricsModule(TimeUnit.SECONDS, TimeUnit.SECONDS, false));
  private final ThreadDump threadDump = new ThreadDump(
    ManagementFactory.getThreadMXBean()
  );

  public AdminController(SingularityPlatform platform) {
    this.platform = platform;
    registerHealthChecks();
  }

  /** The health checks the service reports: the ones its object graph holds, and the deadlock check. */
  private void registerHealthChecks() {
    final HealthCheckRegistry registry = platform.getHealthCheckRegistry();
    if (platform.getInjector() != null) {
      for (Key<?> key : platform.getInjector().getAllBindings().keySet()) {
        final Class<?> type = key.getTypeLiteral().getRawType();
        if (HealthCheck.class.isAssignableFrom(type) && key.getAnnotation() == null) {
          final Binding<?> binding = platform.getInjector().getExistingBinding(key);
          if (binding != null) {
            registry.register(type.getSimpleName(), (HealthCheck) binding.getProvider().get());
          }
        }
      }
    }
    registry.register("deadlocks", new ThreadDeadlockHealthCheck());
  }

  @GetMapping(path = { "", "/" })
  public ResponseEntity<String> menu(HttpServletRequest request) {
    return ResponseEntity
      .ok()
      .header(HttpHeaders.CACHE_CONTROL, NO_CACHE)
      .header(HttpHeaders.CONTENT_TYPE, "text/html;charset=utf-8")
      .body(String.format(MENU, request.getContextPath()));
  }

  @GetMapping("/ping")
  public ResponseEntity<String> ping() {
    return ResponseEntity
      .ok()
      .header(HttpHeaders.CACHE_CONTROL, NO_CACHE)
      .header(HttpHeaders.CONTENT_TYPE, PLAIN_TEXT)
      .body("pong\n");
  }

  @GetMapping("/threads")
  public ResponseEntity<byte[]> threads() {
    final ByteArrayOutputStream written = new ByteArrayOutputStream();
    threadDump.dump(written);
    return ResponseEntity
      .ok()
      .header(HttpHeaders.CACHE_CONTROL, NO_CACHE)
      .header(HttpHeaders.CONTENT_TYPE, "text/plain")
      .body(written.toByteArray());
  }

  @GetMapping("/healthcheck")
  public ResponseEntity<String> healthcheck() throws IOException {
    final SortedMap<String, HealthCheck.Result> results = new TreeMap<>(
      platform.getHealthCheckRegistry().runHealthChecks()
    );
    final boolean healthy = results
      .values()
      .stream()
      .allMatch(HealthCheck.Result::isHealthy);
    final int status = results.isEmpty() ? 501 : healthy ? 200 : 500;
    return ResponseEntity
      .status(status)
      .header(HttpHeaders.CACHE_CONTROL, NO_CACHE)
      .contentType(MediaType.APPLICATION_JSON)
      .body(healthCheckMapper.writer().writeValueAsString(results));
  }

  @GetMapping("/metrics")
  public ResponseEntity<String> metrics() throws IOException {
    return ResponseEntity
      .ok()
      .header(HttpHeaders.CACHE_CONTROL, NO_CACHE)
      .contentType(MediaType.APPLICATION_JSON)
      .body(metricsMapper.writer().writeValueAsString(platform.getMetricRegistry()));
  }

  @GetMapping("/tasks")
  @ResponseBody
  public ResponseEntity<String> tasks() {
    return ResponseEntity
      .ok()
      .header(HttpHeaders.CONTENT_TYPE, PLAIN_TEXT)
      .body("gc\nlog-level\n");
  }

  @PostMapping("/tasks/gc")
  public ResponseEntity<String> runGarbageCollection() {
    System.gc();
    return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, TASK_TEXT).body("");
  }

  @PostMapping("/tasks/log-level")
  public ResponseEntity<String> setLogLevel(HttpServletRequest request) {
    final StringBuilder written = new StringBuilder();
    final Map<String, String[]> parameters = com.hubspot.singularity.web.QueryParameters.of(
      request
    );
    final String[] level = parameters.get("level");
    final String[] loggers = parameters.get("logger");
    if (loggers != null && level != null && level.length > 0) {
      for (String logger : loggers) {
        ((ch.qos.logback.classic.LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory()).getLogger(
            logger
          )
          .setLevel(ch.qos.logback.classic.Level.toLevel(level[0]));
        written
          .append("Configured logging level for ")
          .append(logger)
          .append(" to ")
          .append(level[0])
          .append('\n');
      }
    }
    return ResponseEntity
      .ok()
      .header(HttpHeaders.CONTENT_TYPE, TASK_TEXT)
      .body(written.toString());
  }

  /**
   * A task is run by asking for it to be run, and nothing else: any other method on a task, whether the
   * service has that task or not, is refused with the status and nothing else.
   */
  @RequestMapping(
    path = "/tasks/**",
    method = {
      RequestMethod.GET,
      RequestMethod.PUT,
      RequestMethod.DELETE,
      RequestMethod.PATCH,
      RequestMethod.HEAD
    }
  )
  public ResponseEntity<Void> taskMethod() {
    return ResponseEntity.status(405).contentLength(0).build();
  }

}
