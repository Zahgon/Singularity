package com.hubspot.singularity.resources;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.InactiveAgentManager;
import com.hubspot.singularity.web.Auth;
import com.hubspot.singularity.web.QueryParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.Set;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.INACTIVE_AGENTS_RESOURCE_PATH)
@Schema(title = "Manage Singularity machines that should be marked as inactive")
@Tags({ @Tag(name = "Inactive Machines") })
public class InactiveAgentResource {
  private final InactiveAgentManager inactiveAgentManager;
  private final SingularityAuthorizer authorizationHelper;

  @Inject
  public InactiveAgentResource(
    InactiveAgentManager inactiveAgentManager,
    SingularityAuthorizer authorizationHelper
  ) {
    this.inactiveAgentManager = inactiveAgentManager;
    this.authorizationHelper = authorizationHelper;
  }

  @GetMapping
  @Operation(summary = "Retrieve a list of agents marked as inactive")
  public Set<String> getInactiveSlaves() {
    return inactiveAgentManager.getInactiveAgents();
  }

  @PostMapping
  @Operation(summary = "Mark an agent as inactive")
  public void deactivateSlave(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(required = true, description = "The host to deactivate") @QueryParam(
      "host"
    ) String host
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    inactiveAgentManager.deactivateAgent(host);
  }

  @DeleteMapping
  @Operation(summary = "Remove a host from teh deactivated list")
  public void reactivateSlave(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "The host to remove from the deactivated list"
    ) @QueryParam("host") String host
  ) {
    authorizationHelper.checkAdminAuthorization(user);
    inactiveAgentManager.activateAgent(host);
  }

  @DeleteMapping("/all")
  public void clearAllInactiveHosts(@Auth SingularityUser user) {
    authorizationHelper.checkAdminAuthorization(user);
    inactiveAgentManager.getInactiveAgents().forEach(inactiveAgentManager::activateAgent);
  }
}
