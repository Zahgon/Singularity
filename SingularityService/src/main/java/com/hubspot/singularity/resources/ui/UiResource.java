package com.hubspot.singularity.resources.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.hubspot.singularity.Singularity;
import com.hubspot.singularity.config.IndexViewConfiguration;
import com.hubspot.singularity.resources.SingularityServiceUIModule;
import com.hubspot.singularity.views.IndexView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves as the base for the UI, returns the mustache view for the actual GUI.
 */
@Singleton
@RestController
@RequestMapping(UiResource.UI_RESOURCE_LOCATION + "{uiPath:.*}")
public class UiResource {
  public static final String UI_RESOURCE_LOCATION = "/ui/";

  private final IndexViewConfiguration configuration;
  private final String singularityUriBase;
  private final ObjectMapper mapper;

  @Inject
  public UiResource(
    @Named(SingularityServiceUIModule.SINGULARITY_URI_BASE) String singularityUriBase,
    IndexViewConfiguration configuration,
    @Singularity ObjectMapper mapper
  ) {
    this.configuration = configuration;
    this.singularityUriBase = singularityUriBase;
    this.mapper = mapper;
  }

  @GetMapping(produces = { "text/html" })
  public IndexView getIndex() {
    return new IndexView(singularityUriBase, UI_RESOURCE_LOCATION, configuration, mapper);
  }
}
