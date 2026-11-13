package com.hubspot.singularity.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.AgentPlacement;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.SingularityLimits;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.config.OverrideConfiguration;
import com.hubspot.singularity.config.SingularityConfiguration;
import com.hubspot.singularity.web.Auth;
import com.hubspot.singularity.web.PathParam;
import com.ning.http.client.AsyncHttpClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.CONFIGURATION_RESOURCE_PATH)
@Schema(title = "Exposes some live Singularity configuration")
@Tags({ @Tag(name = "Singularity configuration") })
public class SingularityConfigurationResource extends AbstractLeaderAwareResource {
  private static final Logger LOG = LoggerFactory.getLogger(
    SingularityConfigurationResource.class
  );
  private final SingularityConfiguration config;
  private final SingularityAuthorizer auth;
  private final OverrideConfiguration overrides;

  @Inject
  public SingularityConfigurationResource(
    SingularityConfiguration config,
    OverrideConfiguration overrides,
    SingularityAuthorizer authorizationHelper,
    LeaderLatch leaderLatch,
    AsyncHttpClient httpClient,
    @Singularity ObjectMapper objectMapper
  ) {
    super(httpClient, leaderLatch, objectMapper);
    this.config = config;
    this.overrides = overrides;
    this.auth = authorizationHelper;
  }

  @GetMapping("/singularity-limits")
  @Operation(summary = "Retrieve configuration data for Singularity")
  public SingularityLimits getSingularityLimits() {
    return new SingularityLimits(config.getMaxDecommissioningAgents());
  }

  @PostMapping("/rack-sensitive/enable")
  @Operation(summary = "Enable global rack sensitivity, respecting request settings")
  public ResponseEntity<Object> enableGlobalRackSensitivity(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      null,
      () -> {
        auth.checkAdminAuthorization(user);
        LOG.info("Config override - allowRackSensitivity=true");
        overrides.setAllowRackSensitivity(true);
        return ResponseEntity.ok().build();
      }
    );
  }

  @PostMapping("/rack-sensitive/disable")
  @Operation(summary = "Disable global rack sensitivity, overriding request settings")
  public ResponseEntity<Object> disableGlobalRackSensitivity(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    auth.checkAdminAuthorization(user);
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      null,
      () -> {
        LOG.info("Config override - allowRackSensitivity=false");
        overrides.setAllowRackSensitivity(false);
        return ResponseEntity.ok().build();
      }
    );
  }

  @PostMapping("/placement-strategy/override/set/{strategy}")
  @Operation(
    summary = "Set global placement strategy override, causing scheduling to ignore the default and request settings."
  )
  public ResponseEntity<Object> setPlacementStrategyOverride(
    HttpServletRequest requestContext,
    @Parameter(required = false, description = "Placement strategy name") @PathParam(
      "strategy"
    ) AgentPlacement strategy,
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    auth.checkAdminAuthorization(user);
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      null,
      () -> {
        LOG.info("Config override - agentPlacementOverride={}", strategy);
        overrides.setAgentPlacementOverride(Optional.ofNullable(strategy));
        return ResponseEntity.ok().build();
      }
    );
  }

  @PostMapping("/placement-strategy/override/clear")
  @Operation(
    summary = "Clear global placement strategy override, causing scheduling to respect the default and request settings."
  )
  public ResponseEntity<Object> disableSeparatePlacement(
    HttpServletRequest requestContext,
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    auth.checkAdminAuthorization(user);
    return maybeProxyToLeader(
      requestContext,
      ResponseEntity.class,
      null,
      () -> {
        LOG.info("Config override - agentPlacementOverride=");
        overrides.setAgentPlacementOverride(Optional.empty());
        return ResponseEntity.ok().build();
      }
    );
  }
}
