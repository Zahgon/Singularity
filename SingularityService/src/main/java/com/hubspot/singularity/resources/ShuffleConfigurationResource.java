package com.hubspot.singularity.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.hubspot.singularity.SingularityAuthorizationScope;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.auth.SingularityAuthorizer;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.ShuffleConfigurationManager;
import com.hubspot.singularity.web.Auth;
import com.hubspot.singularity.web.PathParam;
import com.ning.http.client.AsyncHttpClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import java.util.List;
import org.apache.curator.framework.recipes.leader.LeaderLatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.SHUFFLE_RESOURCE_PATH)
@Schema(title = "Manages shuffle configuration of Singularity.")
@Tags({ @Tag(name = "Shuffle") })
public class ShuffleConfigurationResource extends AbstractLeaderAwareResource {
  private static final Logger LOG = LoggerFactory.getLogger(
    ShuffleConfigurationResource.class
  );

  private final ShuffleConfigurationManager shuffleCfgManager;
  private final SingularityAuthorizer authorizationHelper;

  @Inject
  public ShuffleConfigurationResource(
    AsyncHttpClient httpClient,
    LeaderLatch leaderLatch,
    ObjectMapper objectMapper,
    ShuffleConfigurationManager shuffleCfgManager,
    SingularityAuthorizer authorizationHelper
  ) {
    super(httpClient, leaderLatch, objectMapper);
    this.shuffleCfgManager = shuffleCfgManager;
    this.authorizationHelper = authorizationHelper;
  }

  @GetMapping("/blacklist")
  @Operation(summary = "Retrieve the set of request IDs that should not be shuffled.")
  public List<String> getShuffleBlacklist(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    authorizationHelper.checkReadAuthorization(user);
    return shuffleCfgManager.getShuffleBlocklist();
  }

  @GetMapping("/blacklist/{requestId}")
  @Operation(summary = "Check if a request ID is on the shuffle blacklist.")
  public boolean isOnShuffleBlacklist(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "The Request ID to fetch crash loops for"
    ) @PathParam("requestId") String requestId
  ) {
    authorizationHelper.checkForAuthorizationByRequestId(
      requestId,
      user,
      SingularityAuthorizationScope.READ
    );
    return shuffleCfgManager.isOnShuffleBlocklist(requestId);
  }

  @PostMapping("/blacklist/{requestId}")
  @Operation(
    summary = "Add a request ID to the shuffle blacklist. No effect if already present."
  )
  public void addToShuffleBlacklist(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "The Request ID to fetch crash loops for"
    ) @PathParam("requestId") String requestId
  ) {
    authorizationHelper.checkForAuthorizationByRequestId(
      requestId,
      user,
      SingularityAuthorizationScope.WRITE
    );
    shuffleCfgManager.addToShuffleBlocklist(requestId);
  }

  @DeleteMapping("/blacklist/{requestId}")
  @Operation(
    summary = "Remove a request ID from the shuffle blacklist. No effect if not present."
  )
  public void removeFromShuffleBlacklist(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @Parameter(
      required = true,
      description = "The Request ID to fetch crash loops for"
    ) @PathParam("requestId") String requestId
  ) {
    authorizationHelper.checkForAuthorizationByRequestId(
      requestId,
      user,
      SingularityAuthorizationScope.WRITE
    );
    shuffleCfgManager.removeFromShuffleBlocklist(requestId);
  }
}
