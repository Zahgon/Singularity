package com.hubspot.singularity.resources;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityPendingRequest;
import com.hubspot.singularity.SingularityPendingTask;
import com.hubspot.singularity.SingularityTaskHistory;
import com.hubspot.singularity.SingularityTaskId;
import com.hubspot.singularity.SingularityTaskState;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.RequestManager;
import com.hubspot.singularity.data.TaskManager;
import com.hubspot.singularity.data.history.HistoryManager;
import com.hubspot.singularity.web.Auth;
import com.hubspot.singularity.web.PathParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.Collections;
import java.util.Optional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.TASK_TRACKER_RESOURCE_PATH)
@Schema(title = "Retrieve a task by taskId or runId")
@Tags({ @Tag(name = "Task Tracking") })
public class TaskTrackerResource {
  private final TaskManager taskManager;
  private final RequestManager requestManager;
  private final HistoryManager historyManager;
  private final SingularityAuthorizer authorizationHelper;

  @Inject
  public TaskTrackerResource(
    TaskManager taskManager,
    RequestManager requestManager,
    HistoryManager historyManager,
    SingularityAuthorizer authorizationHelper
  ) {
    this.taskManager = taskManager;
    this.requestManager = requestManager;
    this.historyManager = historyManager;
    this.authorizationHelper = authorizationHelper;
  }

  @GetMapping("/task/{taskId}")
  @Operation(
    summary = "Get the current state of a task by taskId whether it is active, or inactive",
    responses = {
      @ApiResponse(responseCode = "404", description = "Task with this id does not exist")
    }
  )
  public Optional<SingularityTaskState> getTaskState(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "the task id to search for") @PathParam(
      "taskId"
    ) String taskId
  ) {
    authorizationHelper.checkForAuthorizationByTaskId(
      taskId,
      user,
      SingularityAuthorizationScope.READ
    );
    return getTaskStateFromId(SingularityTaskId.valueOf(taskId));
  }

  @GetMapping("/run/{requestId}/{runId}")
  @Operation(
    summary = "Get the current state of a task by taskId whether it is pending, active, or inactive",
    responses = {
      @ApiResponse(
        responseCode = "404",
        description = "Task with this runId does not exist"
      )
    }
  )
  public Optional<SingularityTaskState> getTaskStateByRunId(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "the request id to search for tasks"
    ) @PathParam("requestId") String requestId,
    @Parameter(required = true, description = "the run id to search for") @PathParam(
      "runId"
    ) String runId
  ) {
    authorizationHelper.checkForAuthorizationByRequestId(
      requestId,
      user,
      SingularityAuthorizationScope.READ
    );

    // Check if it's active or inactive
    Optional<SingularityTaskId> maybeTaskId = taskManager.getTaskByRunId(
      requestId,
      runId
    );
    if (maybeTaskId.isPresent()) {
      Optional<SingularityTaskState> maybeTaskState = getTaskStateFromId(
        maybeTaskId.get()
      );
      if (maybeTaskState.isPresent()) {
        return maybeTaskState;
      }
    } else {
      Optional<SingularityTaskHistory> maybeTaskHistory = historyManager.getTaskHistoryByRunId(
        requestId,
        runId
      );
      if (maybeTaskHistory.isPresent()) {
        return Optional.of(SingularityTaskState.fromTaskHistory(maybeTaskHistory.get()));
      }
    }
    // Check if it's pending
    for (SingularityPendingTask pendingTask : taskManager.getPendingTasksForRequest(
      requestId,
      false
    )) {
      if (
        pendingTask.getRunId().isPresent() && pendingTask.getRunId().get().equals(runId)
      ) {
        return Optional.of(
          new SingularityTaskState(
            Optional.empty(),
            pendingTask.getPendingTaskId(),
            pendingTask.getRunId(),
            Optional.empty(),
            Collections.emptyList(),
            true
          )
        );
      }
    }

    for (SingularityPendingRequest pendingRequest : requestManager.getPendingRequests()) {
      if (
        pendingRequest.getRequestId().equals(requestId) &&
        pendingRequest.getRunId().isPresent() &&
        pendingRequest.getRunId().get().equals(runId)
      ) {
        return Optional.of(
          new SingularityTaskState(
            Optional.empty(),
            Optional.empty(),
            pendingRequest.getRunId(),
            Optional.empty(),
            Collections.emptyList(),
            true
          )
        );
      }
    }

    return Optional.empty();
  }

  private Optional<SingularityTaskState> getTaskStateFromId(
    SingularityTaskId singularityTaskId
  ) {
    Optional<SingularityTaskHistory> maybeTaskHistory = taskManager.getTaskHistory(
      singularityTaskId
    );
    if (!maybeTaskHistory.isPresent()) {
      maybeTaskHistory = historyManager.getTaskHistory(singularityTaskId.toString());
    }
    if (
      maybeTaskHistory.isPresent() &&
      maybeTaskHistory.get().getLastTaskUpdate().isPresent()
    ) {
      return Optional.of(SingularityTaskState.fromTaskHistory(maybeTaskHistory.get()));
    } else {
      return Optional.empty();
    }
  }
}
