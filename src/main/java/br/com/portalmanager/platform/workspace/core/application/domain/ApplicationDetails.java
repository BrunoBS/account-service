package br.com.portalmanager.platform.workspace.core.application.domain;

import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeCode;
import tools.jackson.databind.JsonNode;

public record ApplicationDetails(
    String name,
    String alias,
    String description,
    String acronym,
    ApplicationScopeTypeCode applicationScope,
    String authorizerGroup,
    JsonNode settings
) {}
