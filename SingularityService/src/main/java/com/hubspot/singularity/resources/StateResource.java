package com.hubspot.singularity.resources;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityScheduledTasksInfo;
import com.hubspot.singularity.SingularityState;
import com.hubspot.singularity.SingularityTaskReconciliationStatistics;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.StateManager;
import com.hubspot.singularity.web.QueryParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.STATE_RESOURCE_PATH)
@Schema(title = "Provides information about the current state of Singularity")
@Tags({ @Tag(name = "State") })
public class StateResource {
  private final StateManager stateManager;

  @Inject
  public StateResource(StateManager stateManager) {
    this.stateManager = stateManager;
  }

  @GetMapping("/scheduled-tasks-info")
  @Operation(summary = "Retrieve the scheduled tasks info.")
  public SingularityScheduledTasksInfo getScheduledTasksInfo() {
    return stateManager.getScheduledTasksInfo();
  }

  @GetMapping
  @Operation(summary = "Retrieve information about the current state of Singularity.")
  public SingularityState getState(
    @QueryParam("skipCache") boolean skipCache,
    @QueryParam("includeRequestIds") boolean includeRequestIds
  ) {
    return stateManager.getState(skipCache, includeRequestIds);
  }

  @GetMapping("/requests/under-provisioned")
  @Operation(summary = "Retrieve the list of under-provisioned request IDs.")
  public List<String> getUnderProvisionedRequestIds(
    @QueryParam("skipCache") boolean skipCache
  ) {
    return stateManager.getState(skipCache, true).getUnderProvisionedRequestIds();
  }

  @GetMapping("/requests/over-provisioned")
  @Operation(summary = "Retrieve the list of over-provisioned request IDs.")
  public List<String> getOverProvisionedRequestIds(
    @QueryParam("skipCache") boolean skipCache
  ) {
    return stateManager.getState(skipCache, true).getOverProvisionedRequestIds();
  }

  @GetMapping("/task-reconciliation")
  @Operation(
    summary = "Retrieve information about the most recent task reconciliation",
    responses = {
      @ApiResponse(
        responseCode = "404",
        description = "No reconciliation statistics are present"
      )
    }
  )
  public Optional<SingularityTaskReconciliationStatistics> getTaskReconciliationStatistics() {
    return stateManager.getTaskReconciliationStatistics();
  }
}
