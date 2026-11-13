package com.hubspot.singularity.resources;

import com.google.inject.Inject;
import com.hubspot.singularity.SingularityUser;
import com.hubspot.singularity.SingularityUserSettings;
import com.hubspot.singularity.config.ApiPaths;
import com.hubspot.singularity.data.UserManager;
import com.hubspot.singularity.web.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.tags.Tags;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.USER_RESOURCE_PATH)
@Schema(title = "Retrieve or update settings for a particular user")
@Tags({ @Tag(name = "Users") })
public class UserResource {
  private final UserManager userManager;

  @Inject
  public UserResource(UserManager userManager) {
    this.userManager = userManager;
  }

  @GetMapping("/settings")
  @Operation(summary = "Retrieve the settings for the current authenticated user")
  public SingularityUserSettings getUserSettings(
    @Parameter(hidden = true) @Auth SingularityUser user
  ) {
    return userManager
      .getUserSettings(user.getId())
      .orElse(SingularityUserSettings.empty());
  }

  @PostMapping("/settings")
  @Operation(summary = "Update the settings for the current authenticated user")
  public void setUserSettings(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "The new settings to be saved for the currently authenticated user"
    ) SingularityUserSettings settings
  ) {
    userManager.updateUserSettings(user.getId(), settings);
  }

  @PostMapping(path = "/settings/starred-requests")
  @Operation(summary = "Add starred requests for the current authenticated user")
  public void addStarredRequests(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "A SingularityUserSettings object containing the new starred requests for the currently authenticated user"
    ) SingularityUserSettings settings
  ) {
    userManager.addStarredRequestIds(user.getId(), settings.getStarredRequestIds());
  }

  @DeleteMapping(path = "/settings/starred-requests")
  @Operation(summary = "Remove starred requests for the current authenticated user")
  public void deleteStarredRequests(
    @Parameter(hidden = true) @Auth SingularityUser user,
    @org.springframework.web.bind.annotation.RequestBody(required = false)
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
      required = true,
      description = "A SingularityUserSettings object containing starred requests to remove for the currently authenticated user"
    ) SingularityUserSettings settings
  ) {
    userManager.deleteStarredRequestIds(user.getId(), settings.getStarredRequestIds());
  }
}
