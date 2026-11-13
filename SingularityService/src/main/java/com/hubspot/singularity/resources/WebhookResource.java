package com.hubspot.singularity.resources;

import com.google.inject.Inject;
import com.hubspot.mesos.JavaUtils;
import com.hubspot.singularity.ElevatedAccessEvent;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityCreateResult;
import com.hubspot.singularity.SingularityDeleteResult;
import com.hubspot.singularity.SingularityDeployUpdate;
import com.hubspot.singularity.SingularityRequestHistory;
import com.hubspot.singularity.SingularityTaskHistoryUpdate;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SingularityWebhook;
import com.hubspot.singularity.SingularityWebhookSummary;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.data.WebhookManager;
import com.hubspot.singularity.web.Auth;
import com.hubspot.singularity.web.PathParam;
import com.hubspot.singularity.web.QueryParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.WEBHOOK_RESOURCE_PATH)
@Schema(title = "Manage Singularity webhooks")
@Tags({ @Tag(name = "Webhooks") })
public class WebhookResource {
  private final WebhookManager webhookManager;
  private final SingularityAuthorizer authorizationHelper;
  private final SingularityValidator validator;

  @Inject
  public WebhookResource(
    WebhookManager webhookManager,
    SingularityAuthorizer authorizationHelper,
    SingularityValidator validator
  ) {
    this.webhookManager = webhookManager;
    this.authorizationHelper = authorizationHelper;
    this.validator = validator;
  }

  @GetMapping
  @Operation(summary = "Retrieve a list of active webhooks.")
  public List<SingularityWebhook> getActiveWebhooks(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getActiveWebhooksUncached();
  }

  @GetMapping("/summary")
  @Operation(summary = "Retrieve a summary of each active webhook")
  public List<SingularityWebhookSummary> getWebhooksWithQueueSize(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getWebhooksWithQueueSize();
  }

  @PostMapping
  @Operation(
    summary = "Add a new webhook",
    responses = {
      @ApiResponse(
        responseCode = "409",
        description = "Adding new webhooks is currently disabled"
      )
    }
  )
  public SingularityCreateResult addWebhook(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "SingularityWebhook object describing the new webhook to be added"
    ) SingularityWebhook webhook
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(SingularityAction.ADD_WEBHOOK);
    validator.checkSingularityWebhook(webhook);
    return webhookManager.addWebhook(webhook);
  }

  @DeleteMapping("/{webhookId}")
  @Deprecated
  @Operation(
    summary = "Delete a specific webhook",
    responses = {
      @ApiResponse(
        responseCode = "409",
        description = "Deleting webhooks is currently disabled"
      )
    }
  )
  public SingularityDeleteResult deleteWebhookDeprecated(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Id of the webhook to delete") @PathParam(
      "webhookId"
    ) String webhookId
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(SingularityAction.REMOVE_WEBHOOK);
    return webhookManager.deleteWebhook(JavaUtils.urlEncode(webhookId));
  }

  @GetMapping("/deploy/{webhookId}")
  @Deprecated
  @Operation(summary = "Retrieve a list of queued deploy updates for a specific webhook")
  public List<SingularityDeployUpdate> getQueuedDeployUpdatesDeprecated(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "Id of the webhook to get deploy updates for"
    ) @PathParam("webhookId") String webhookId
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedDeployUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

  @GetMapping("/request/{webhookId}")
  @Deprecated
  @Operation(summary = "Retrieve a list of queued request updates for a specific webhook")
  public List<SingularityRequestHistory> getQueuedRequestUpdatesDeprecated(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "Id of the webhook to get request updates for"
    ) @PathParam("webhookId") String webhookId
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedRequestHistoryForHook(JavaUtils.urlEncode(webhookId));
  }

  @GetMapping("/task/{webhookId}")
  @Deprecated
  @Operation(summary = "Retrieve a list of queued task updates for a specific webhook")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdatesDeprecated(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "Id of the webhook to get task updates for"
    ) @PathParam("webhookId") String webhookId
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedTaskUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

  @DeleteMapping
  @Operation(summary = "Delete a specific webhook by id")
  public SingularityDeleteResult deleteWebhook(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Id of the webhook to delete") @QueryParam(
      "webhookId"
    ) String webhookId
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    validator.checkActionEnabled(SingularityAction.REMOVE_WEBHOOK);
    return webhookManager.deleteWebhook(JavaUtils.urlEncode(webhookId));
  }

  @GetMapping("/deploy")
  @Operation(summary = "Retrieve a list of queued deploy updates for a specific webhook")
  public List<SingularityDeployUpdate> getQueuedDeployUpdates(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "Id of the webhook to get deploy updates for"
    ) @QueryParam("webhookId") String webhookId
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedDeployUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

  @GetMapping("/request")
  @Operation(
    summary = "Retrieve a list of queued request updates for a specific webhook."
  )
  public List<SingularityRequestHistory> getQueuedRequestUpdates(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(description = "Id of the webhook to get request updates for") @QueryParam(
      "webhookId"
    ) String webhookId
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedRequestHistoryForHook(JavaUtils.urlEncode(webhookId));
  }

  @GetMapping("/task")
  @Operation(summary = "Retrieve a list of queued task updates for a specific webhook.")
  public List<SingularityTaskHistoryUpdate> getQueuedTaskUpdates(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "Id of the webhook to get task updates for"
    ) @QueryParam("webhookId") String webhookId
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getQueuedTaskUpdatesForHook(JavaUtils.urlEncode(webhookId));
  }

  @GetMapping("/elevated-access")
  @Operation(
    summary = "Retrieve a list of queued elevated access updates for a specific webhook."
  )
  public List<ElevatedAccessEvent> getQueuedElevatedAccessEventUpdates(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "Id of the webhook to get task updates for"
    ) @QueryParam("webhookId") String webhookId
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return webhookManager.getElevatedAccessEventForHook(JavaUtils.urlEncode(webhookId));
  }
}
