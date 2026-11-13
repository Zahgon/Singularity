package com.hubspot.singularity.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.MachineState;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityAction;
import com.hubspot.singularity.SingularityAgent;
import com.hubspot.singularity.SingularityMachineStateHistoryUpdate;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.api.SingularityMachineChangeRequest;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.AgentManager;
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

/**
 * @deprecated use {@link AgentResource}
 */
@RestController
@RequestMapping(ApiPaths.SLAVE_RESOURCE_PATH)
@Schema(title = "Manages Singularity agents")
@Tags({ @Tag(name = "Agents") })
@Deprecated
public class AgentResourceDeprecated extends AbstractMachineResource<SingularityAgent> {

  @Inject
  public AgentResourceDeprecated(
    AsyncHttpClient httpClient,
    LeaderLatch leaderLatch,
    @Singularity ObjectMapper objectMapper,
    AgentManager agentManager,
    SingularityAuthorizer authorizationHelper,
    SingularityValidator validator
  ) {
    super(
      httpClient,
      leaderLatch,
      objectMapper,
      agentManager,
      authorizationHelper,
      validator
    );
  }

  @Override
  protected String getObjectTypeString() {
    return "Slave";
  }

  @GetMapping("/")
  @Operation(
    summary = "Retrieve the list of all known slaves, optionally filtering by a particular state"
  )
  public List<SingularityAgent> getSlaves(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      description = "Optionally specify a particular state to filter slaves by"
    ) @QueryParam("state") Optional<MachineState> filterState
  ) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getObjectsFiltered(filterState);
  }

  @GetMapping("/slave/{slaveId}")
  @Operation(summary = "Retrieve the history of a given slave")
  public List<SingularityMachineStateHistoryUpdate> getSlaveHistory(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Slave ID") @PathParam(
      "slaveId"
    ) String slaveId
  ) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getHistory(slaveId);
  }

  @GetMapping("/slave/{slaveId}/details")
  @Operation(summary = "Get information about a particular slave")
  public Optional<SingularityAgent> getSlave(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Slave ID") @PathParam(
      "slaveId"
    ) String slaveId
  ) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getObject(slaveId);
  }

  @DeleteMapping("/slave/{slaveId}")
  @Operation(
    summary = "Remove a known slave, erasing history. This operation will cancel decomissioning of the slave"
  )
  public ResponseEntity<Object> removeSlave(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Active SlaveId") @PathParam(
      "slaveId"
    ) String slaveId
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      null,
      () -> {
        super.remove(slaveId, user);
        return ResponseEntity.ok().build();
      }
    );
  }

  @PostMapping(path = "/slave/{slaveId}/decommission")
  @Operation(summary = "Begin decommissioning a specific active slave")
  public ResponseEntity<Object> decommissionSlave(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Active slaveId") @PathParam(
      "slaveId"
    ) String slaveId,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Settings related to changing the state of a slave"
    ) SingularityMachineChangeRequest changeRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      changeRequest,
      () -> {
        decommissionSlave(user, slaveId, changeRequest);
        return ResponseEntity.ok().build();
      }
    );
  }

  public void decommissionSlave(
    SingularityUser user,
    String slaveId,
    SingularityMachineChangeRequest changeRequest
  ) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.ofNullable(
      changeRequest
    );
    super.decommission(
      slaveId,
      maybeChangeRequest,
      user,
      SingularityAction.DECOMMISSION_AGENT
    );
  }

  @PostMapping(path = "/slave/{slaveId}/freeze")
  @Operation(summary = "Freeze tasks on a specific slave")
  public ResponseEntity<Object> freezeSlave(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Slave ID") @PathParam(
      "slaveId"
    ) String slaveId,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Settings related to changing the state of a slave"
    ) SingularityMachineChangeRequest changeRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      changeRequest,
      () -> {
        freezeSlave(user, slaveId, changeRequest);
        return ResponseEntity.ok().build();
      }
    );
  }

  public void freezeSlave(
    SingularityUser user,
    String slaveId,
    SingularityMachineChangeRequest changeRequest
  ) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.ofNullable(
      changeRequest
    );
    super.freeze(slaveId, maybeChangeRequest, user, SingularityAction.FREEZE_AGENT);
  }

  @PostMapping(path = "/slave/{slaveId}/activate")
  @Operation(
    summary = "Activate a decomissioning slave, canceling decomission without erasing history"
  )
  public ResponseEntity<Object> activateSlave(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Active slaveId") @PathParam(
      "slaveId"
    ) String slaveId,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Settings related to changing the state of a slave"
    ) SingularityMachineChangeRequest changeRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      changeRequest,
      () -> {
        activateSlave(user, slaveId, changeRequest);
        return ResponseEntity.ok().build();
      }
    );
  }

  public void activateSlave(
    SingularityUser user,
    String slaveId,
    SingularityMachineChangeRequest changeRequest
  ) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.ofNullable(
      changeRequest
    );
    super.activate(slaveId, maybeChangeRequest, user, SingularityAction.ACTIVATE_AGENT);
  }

  @DeleteMapping("/slave/{slaveId}/expiring")
  @Operation(summary = "Delete any expiring machine state changes for this slave")
  public ResponseEntity<Object> deleteExpiringStateChange(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Active slaveId") @PathParam(
      "slaveId"
    ) String slaveId
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      null,
      () -> {
        super.cancelExpiring(slaveId, user);
        return ResponseEntity.ok().build();
      }
    );
  }

  @GetMapping("/expiring")
  @Operation(summary = "Get all expiring state changes for all slaves")
  public List<SingularityExpiringMachineState> getExpiringStateChanges(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    return super.getExpiringStateChanges(user);
  }

  @DeleteMapping("/dead")
  public ResponseEntity<Object> clearAllDeadSlaves(
    HttpServletRequest requestContext,
    @Auth SingularityUser user
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      null,
      () -> {
        authorizationHelper.checkAdminAuthorization(user);
        manager
          .getObjectsFiltered(MachineState.DEAD)
          .forEach(s -> manager.deleteObject(s.getId()));
        return ResponseEntity.ok().build();
      }
    );
  }
}
