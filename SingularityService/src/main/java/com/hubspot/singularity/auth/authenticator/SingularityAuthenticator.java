package com.hubspot.singularity.auth.authenticator;

import com.hubspot.singularity.web.SingularityRequestContext;
import com.hubspot.singularity.SingularityUser;
import java.util.Optional;

public interface SingularityAuthenticator {
  Optional<SingularityUser> getUser(SingularityRequestContext context);
}
