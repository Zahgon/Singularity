package com.hubspot.singularity.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityRack;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.RackManager;
import com.hubspot.singularity.data.SingularityValidator;
import com.hubspot.singularity.expiring.SingularityExpiringMachineState;
import com.hubspot.singularity.web.Auth;
import com.hubspot.singularity.web.PathParam;
import com.hubspot.singularity.web.QueryParam;
import com.ning.http.client.AsyncHttpClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RequestMapping(ApiPaths.RACK_RESOURCE_PATH)
@Schema(title = "Manage Singularity racks")
@Tags({ @Tag(name = "Racks") })
public class RackResource extends AbstractMachineResource<SingularityRack> {

  @Inject
  public RackResource(
    AsyncHttpClient httpClient,
    LeaderLatch leaderLatch,
    @Singularity ObjectMapper objectMapper,
    RackManager rackManager,
    SingularityAuthorizer authorizationHelper,
    SingularityValidator validator
  ) {
    super(
      httpClient,
      leaderLatch,
      objectMapper,
      rackManager,
      authorizationHelper,
      validator
    );
  }

  @Override
  protected String getObjectTypeString() {
    return "Rack";
  }

  @GetMapping("/")
  @Operation(
    summary = "Retrieve the list of all known racks, optionally filtering by a particular state"
  )
  public List<SingularityRack> getRacks(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      description = "Optionally specify a particular state to filter racks by"
    ) @QueryParam("state") Optional<MachineState> filterState
  ) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getObjectsFiltered(filterState);
  }

  @GetMapping("/rack/{rackId}")
  @Operation(summary = "Retrieve the history of a given rack")
  public List<SingularityMachineStateHistoryUpdate> getRackHistory(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Rack ID") @PathParam(
      "rackId"
    ) String rackId
  ) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getHistory(rackId);
  }

  @DeleteMapping("/rack/{rackId}")
  @Operation(
    summary = "Remove a known rack, erasing history. This operation will cancel decommissioning of racks"
  )
  public ResponseEntity<Object> removeRack(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Rack ID") @PathParam(
      "rackId"
    ) String rackId
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      null,
      () -> {
        super.remove(rackId, user);
        return ResponseEntity.ok().build();
      }
    );
  }

  @PostMapping("/rack/{rackId}/decommission")
  @Operation(summary = "Begin decommissioning a specific active rack")
  public ResponseEntity<Object> decommissionRack(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Rack ID") @PathParam(
      "rackId"
    ) String rackId,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Settings related to changing the state of a rack"
    ) SingularityMachineChangeRequest changeRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      changeRequest,
      () -> {
        final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.ofNullable(
          changeRequest
        );
        super.decommission(
          rackId,
          maybeChangeRequest,
          user,
          SingularityAction.DECOMMISSION_RACK
        );
        return ResponseEntity.ok().build();
      }
    );
  }

  @PostMapping("/rack/{rackId}/freeze")
  @Operation(summary = "Freeze a specific rack")
  public ResponseEntity<Object> freezeRack(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Rack ID") @PathParam(
      "rackId"
    ) String rackId,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Settings related to changing the state of a rack"
    ) SingularityMachineChangeRequest changeRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      changeRequest,
      () -> {
        final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.ofNullable(
          changeRequest
        );
        super.freeze(rackId, maybeChangeRequest, user, SingularityAction.FREEZE_RACK);
        return ResponseEntity.ok().build();
      }
    );
  }

  @PostMapping("/rack/{rackId}/activate")
  @Operation(
    summary = "Activate a decomissioning rack, canceling decomission without erasing history"
  )
  public ResponseEntity<Object> activateRack(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Rack ID") @PathParam(
      "rackId"
    ) String rackId,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Settings related to changing the state of a rack"
    ) SingularityMachineChangeRequest changeRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      changeRequest,
      () -> {
        final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.ofNullable(
          changeRequest
        );
        super.activate(rackId, maybeChangeRequest, user, SingularityAction.ACTIVATE_RACK);
        return ResponseEntity.ok().build();
      }
    );
  }

  @DeleteMapping("/rack/{rackId}/expiring")
  @Operation(summary = "Delete any expiring machine state changes for this rack")
  public ResponseEntity<Object> deleteExpiringStateChange(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Rack ID") @PathParam(
      "rackId"
    ) String rackId
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      null,
      () -> {
        super.cancelExpiring(rackId, user);
        return ResponseEntity.ok().build();
      }
    );
  }

  @GetMapping("/expiring")
  @Operation(summary = "Get all expiring state changes for all racks")
  public List<SingularityExpiringMachineState> getExpiringStateChanges(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    return super.getExpiringStateChanges(user);
  }
}
