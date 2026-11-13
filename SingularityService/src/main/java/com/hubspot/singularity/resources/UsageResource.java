package com.hubspot.singularity.resources;

import com.google.inject.Inject;
import com.hubspot.singularity.RequestUtilization;
import com.hubspot.singularity.SingularityAgentUsageWithId;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityClusterUtilization;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskUsage;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.WebExceptions;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.usage.UsageManager;
import com.hubspot.singularity.web.Auth;
import com.hubspot.singularity.web.PathParam;
import com.hubspot.singularity.web.QueryParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.USAGE_RESOURCE_PATH)
@Schema(title = "Retrieve usage data about slaves and tasks")
@Tags({ @Tag(name = "Resource Usage") })
public class UsageResource {
  private final UsageManager usageManager;
  private final SingularityAuthorizer authorizationHelper;

  @Inject
  public UsageResource(
    UsageManager usageManager,
    SingularityAuthorizer authorizationHelper
  ) {
    this.usageManager = usageManager;
    this.authorizationHelper = authorizationHelper;
  }

  @GetMapping("/slaves")
  @Operation(summary = "Retrieve a list of agent resource usage models with agent ids")
  @Deprecated
  public Collection<SingularityAgentUsageWithId> getAgentsWithUsageDeprecated(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    return getAgentsWithUsage(user);
  }

  @GetMapping("/agents")
  @Operation(summary = "Retrieve a list of agent resource usage models with agent ids")
  public Collection<SingularityAgentUsageWithId> getAgentsWithUsage(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    return usageManager.getAllCurrentAgentUsage().values();
  }

  @GetMapping("/tasks/{taskId}/history")
  @Operation(
    summary = "Retrieve the usage history for a particular task",
    description = "Empty if the task usage has not been collected or has been cleaned up"
  )
  public List<SingularityTaskUsage> getTaskUsageHistory(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "The id of the task to retrieve usage history for"
    ) @PathParam("taskId") String taskId
  ) {
    authorizationHelper.checkForAuthorizationByTaskId(
      taskId,
      user,
      SingularityAuthorizationScope.READ
    );
    return usageManager.getTaskUsage(SingularityTaskId.valueOf(taskId));
  }

  @GetMapping("/cluster/utilization")
  @Operation(
    summary = "GET a summary of utilization for all slaves and requests in the mesos cluster"
  )
  public SingularityClusterUtilization getClusterUtilization(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    //authorizationHelper.checkAdminAuthorization(user); Needed for ui pages outside single request
    WebExceptions.checkNotFound(
      usageManager.getClusterUtilization().isPresent(),
      "No cluster utilization has been saved yet"
    );

    return usageManager.getClusterUtilization().get();
  }

  @GetMapping("/requests")
  public List<RequestUtilization> getRequestUtilizations(
    @Auth SingularityUser user,
    @QueryParam("useWebCache") Boolean useWebCache
  ) {
    return new ArrayList<>(
      usageManager.getRequestUtilizations(useWebCache != null && useWebCache).values()
    );
  }

  @GetMapping("/requests/request/{requestId}")
  public Optional<RequestUtilization> getRequestUtilization(
    @Auth SingularityUser user,
    @PathParam("requestId") String requestId,
    @QueryParam("useWebCache") Boolean useWebCache
  ) {
    authorizationHelper.checkForAuthorizationByRequestId(
      requestId,
      user,
      SingularityAuthorizationScope.READ
    );
    return usageManager.getRequestUtilization(
      requestId,
      useWebCache != null && useWebCache
    );
  }
}
