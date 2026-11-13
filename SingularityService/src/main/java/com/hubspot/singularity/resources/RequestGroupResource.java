package com.hubspot.singularity.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityRequestGroup;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.RequestGroupManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.web.PathParam;
import com.hubspot.singularity.web.QueryParam;
import com.ning.http.client.AsyncHttpClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.REQUEST_GROUP_RESOURCE_PATH)
@Schema(
  title = "Manages Singularity Request Groups, which are collections of one or more Singularity Requests"
)
@Tags({ @Tag(name = "Request Groups") })
public class RequestGroupResource extends AbstractLeaderAwareResource {
  private final RequestGroupManager requestGroupManager;
  private final SingularityValidator validator;

  @Inject
  public RequestGroupResource(
    AsyncHttpClient httpClient,
    LeaderLatch leaderLatch,
    @Singularity ObjectMapper objectMapper,
    RequestGroupManager requestGroupManager,
    SingularityValidator validator
  ) {
    super(httpClient, leaderLatch, objectMapper);
    this.requestGroupManager = requestGroupManager;
    this.validator = validator;
  }

  @GetMapping
  @Operation(summary = "Get a list of Singularity request groups")
  public List<SingularityRequestGroup> getRequestGroupIds(
    @Parameter(
      description = "Use a cached version of this data to limit expensive api calls"
    ) @QueryParam("useWebCache") Boolean useWebCache
  ) {
    return requestGroupManager.getRequestGroups(useWebCache != null && useWebCache);
  }

  @GetMapping("/group/{requestGroupId}")
  @Operation(
    summary = "Get a specific Singularity request group by ID",
    responses = {
      @ApiResponse(
        responseCode = "404",
        description = "The specified request group was not found"
      )
    }
  )
  public Optional<SingularityRequestGroup> getRequestGroup(
    @Parameter(required = true, description = "The id of the request group") @PathParam(
      "requestGroupId"
    ) String requestGroupId
  ) {
    return requestGroupManager.getRequestGroup(requestGroupId);
  }

  @DeleteMapping("/group/{requestGroupId}")
  @Operation(summary = "Delete a specific Singularity request group by ID")
  public ResponseEntity<Object> deleteRequestGroup(
    @Parameter(required = true, description = "The id of the request group") @PathParam(
      "requestGroupId"
    ) String requestGroupId,
    HttpServletRequest requestContext
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      null,
      () -> {
        requestGroupManager.deleteRequestGroup(requestGroupId);
        return ResponseEntity.ok().build();
      }
    );
  }

  @PostMapping
  @Operation(summary = "Create a Singularity request group")
  public SingularityRequestGroup saveRequestGroup(
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "The new request group to create"
    ) SingularityRequestGroup requestGroup,
    HttpServletRequest requestContext
  ) {
    return maybeProxyToLeader(
      requestContext,
      SingularityRequestGroup.class,
      requestGroup,
      () -> {
        validator.checkRequestGroup(requestGroup);
        requestGroupManager.saveRequestGroup(requestGroup);
        return requestGroup;
      }
    );
  }
}
