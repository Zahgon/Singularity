package com.hubspot.singularity.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.NotificationsManager;
import com.hubspot.singularity.web.Auth;
import com.ning.http.client.AsyncHttpClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.NOTIFICATIONS_RESOURCE_PATH)
@Schema(title = "Manage email notifications")
@Tags({ @Tag(name = "Notifications") })
public class NotificationsResource extends AbstractLeaderAwareResource {
  private final NotificationsManager notificationsManager;

  @Inject
  public NotificationsResource(
    AsyncHttpClient httpClient,
    LeaderLatch leaderLatch,
    @Singularity ObjectMapper objectMapper,
    NotificationsManager notificationsManager
  ) {
    super(httpClient, leaderLatch, objectMapper);
    this.notificationsManager = notificationsManager;
  }

  @GetMapping("/blocklist")
  @Operation(summary = "Retrieve the list of blocklisted emails")
  public List<String> getBlacklist(@Parameter(hidden = true) @Auth SingularityUser user) {
    return notificationsManager.getBlocklist();
  }

  @GetMapping("/allowlist")
  @Operation(summary = "Retrieve the list of emails subscribed to Singularity")
  public List<String> getAllowlist(@Parameter(hidden = true) @Auth SingularityUser user) {
    return notificationsManager.getAllowlist();
  }

  @PostMapping("/unsubscribe")
  @Operation(summary = "Unsubscribe from Singularity emails.")
  public void unsubscribe(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "The email address to unsubscribe"
    ) String email,
    HttpServletRequest requestContext
  ) {
    notificationsManager.unsubscribe(getFormattedEmail(email));
  }

  @PostMapping("/subscribe")
  @Operation(summary = "Subscribe to Singularity emails")
  public void subscribe(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "The email address to subscribe"
    ) String email,
    HttpServletRequest requestContext
  ) {
    notificationsManager.subscribe(getFormattedEmail(email));
  }

  private String getFormattedEmail(String email) {
    return email.replaceAll("^\"|\"$", "");
  }
}
