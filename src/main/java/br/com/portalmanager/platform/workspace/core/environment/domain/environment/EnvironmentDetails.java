package br.com.portalmanager.platform.workspace.core.environment.domain.environment;

import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationTypeCode;
import tools.jackson.databind.JsonNode;

public record EnvironmentDetails(
    String name,
    String description,
    AuthorizationTypeCode authorizationType,
    String authorizerGroup,
    JsonNode settings,
    Integer sortOrder
) {}
