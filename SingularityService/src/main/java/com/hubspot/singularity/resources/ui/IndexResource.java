package com.hubspot.singularity.resources.ui;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.hubspot.singularity.resources.SingularityServiceUIModule;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class IndexResource {
  private final String singularityUriBase;

  @Inject
  public IndexResource(
    @Named(SingularityServiceUIModule.SINGULARITY_URI_BASE) String singularityUriBase
  ) {
    this.singularityUriBase = singularityUriBase;
  }

  @GetMapping("/")
  public ResponseEntity<Object> getIndex() {
    return ResponseEntity
      .status(301)
      .location(URI.create(singularityUriBase + UiResource.UI_RESOURCE_LOCATION))
      .build();
  }
}
