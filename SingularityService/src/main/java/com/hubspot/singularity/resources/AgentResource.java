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

@RestController
@RequestMapping(ApiPaths.AGENT_RESOURCE_PATH)
@Schema(title = "Manages Singularity agents")
@Tags({ @Tag(name = "Agents") })
public class AgentResource extends AbstractMachineResource<SingularityAgent> {

  @Inject
  public AgentResource(
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
    return "Agent";
  }

  @GetMapping("/")
  @Operation(
    summary = "Retrieve the list of all known agents, optionally filtering by a particular state"
  )
  public List<SingularityAgent> getAgents(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      description = "Optionally specify a particular state to filter agents by"
    ) @QueryParam("state") Optional<MachineState> filterState
  ) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getObjectsFiltered(filterState);
  }

  @GetMapping("/agent/{agentId}")
  @Operation(summary = "Retrieve the history of a given agent")
  public List<SingularityMachineStateHistoryUpdate> getAgentHistory(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Agent ID") @PathParam(
      "agentId"
    ) String agentId
  ) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getHistory(agentId);
  }

  @GetMapping("/agent/{agentId}/details")
  @Operation(summary = "Get information about a particular agent")
  public Optional<SingularityAgent> getAgent(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Agent ID") @PathParam(
      "agentId"
    ) String agentId
  ) {
    authorizationHelper.checkReadAuthorization(user);
    return manager.getObject(agentId);
  }

  @DeleteMapping("/agent/{agentId}")
  @Operation(
    summary = "Remove a known agent, erasing history. This operation will cancel decomissioning of the agent"
  )
  public ResponseEntity<Object> removeAgent(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Active AgentId") @PathParam(
      "agentId"
    ) String agentId
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      null,
      () -> {
        super.remove(agentId, user);
        return ResponseEntity.ok().build();
      }
    );
  }

  @PostMapping(path = "/agent/{agentId}/decommission")
  @Operation(summary = "Begin decommissioning a specific active agent")
  public ResponseEntity<Object> decommissionAgent(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Active agentId") @PathParam(
      "agentId"
    ) String agentId,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Settings related to changing the state of a agent"
    ) SingularityMachineChangeRequest changeRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      changeRequest,
      () -> {
        decommissionAgent(user, agentId, changeRequest);
        return ResponseEntity.ok().build();
      }
    );
  }

  public void decommissionAgent(
    SingularityUser user,
    String agentId,
    SingularityMachineChangeRequest changeRequest
  ) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.ofNullable(
      changeRequest
    );
    super.decommission(
      agentId,
      maybeChangeRequest,
      user,
      SingularityAction.DECOMMISSION_AGENT
    );
  }

  @PostMapping(path = "/agent/{agentId}/decommission/update")
  @Operation(summary = "Update decommission of a specific active agent")
  public ResponseEntity<Object> updateDecommissionAgent(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Active agentId") @PathParam(
      "agentId"
    ) String agentId,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Settings related to changing the state of a agent"
    ) SingularityMachineChangeRequest changeRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      changeRequest,
      () -> {
        super.updateDecommissionAgent(user, agentId, changeRequest);
        return ResponseEntity.ok().build();
      }
    );
  }

  @PostMapping(path = "/agent/{agentId}/freeze")
  @Operation(summary = "Freeze tasks on a specific agent")
  public ResponseEntity<Object> freezeAgent(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Agent ID") @PathParam(
      "agentId"
    ) String agentId,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Settings related to changing the state of a agent"
    ) SingularityMachineChangeRequest changeRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      changeRequest,
      () -> {
        freezeAgent(user, agentId, changeRequest);
        return ResponseEntity.ok().build();
      }
    );
  }

  public void freezeAgent(
    SingularityUser user,
    String agentId,
    SingularityMachineChangeRequest changeRequest
  ) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.ofNullable(
      changeRequest
    );
    super.freeze(agentId, maybeChangeRequest, user, SingularityAction.FREEZE_AGENT);
  }

  @PostMapping(path = "/agent/{agentId}/activate")
  @Operation(
    summary = "Activate a decomissioning agent, canceling decomission without erasing history"
  )
  public ResponseEntity<Object> activateAgent(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Active agentId") @PathParam(
      "agentId"
    ) String agentId,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      description = "Settings related to changing the state of a agent"
    ) SingularityMachineChangeRequest changeRequest
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      changeRequest,
      () -> {
        activateAgent(user, agentId, changeRequest);
        return ResponseEntity.ok().build();
      }
    );
  }

  public void activateAgent(
    SingularityUser user,
    String agentId,
    SingularityMachineChangeRequest changeRequest
  ) {
    final Optional<SingularityMachineChangeRequest> maybeChangeRequest = Optional.ofNullable(
      changeRequest
    );
    super.activate(agentId, maybeChangeRequest, user, SingularityAction.ACTIVATE_AGENT);
  }

  @DeleteMapping("/agent/{agentId}/expiring")
  @Operation(summary = "Delete any expiring machine state changes for this agent")
  public ResponseEntity<Object> deleteExpiringStateChange(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "Active agentId") @PathParam(
      "agentId"
    ) String agentId
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      null,
      () -> {
        super.cancelExpiring(agentId, user);
        return ResponseEntity.ok().build();
      }
    );
  }

  @GetMapping("/expiring")
  @Operation(summary = "Get all expiring state changes for all agents")
  public List<SingularityExpiringMachineState> getExpiringStateChanges(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    return super.getExpiringStateChanges(user);
  }

  @DeleteMapping("/dead")
  public ResponseEntity<Object> clearAllDeadAgents(
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
