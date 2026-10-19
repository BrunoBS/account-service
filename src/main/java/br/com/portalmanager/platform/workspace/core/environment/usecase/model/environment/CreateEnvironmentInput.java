package br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment;

import tools.jackson.databind.JsonNode;

public record CreateEnvironmentInput(
    Long version,
    String name,
    String description,
    String authorizationType,
    Integer sortOrder,
    String authorizerGroup,
    JsonNode settings,
    String environmentTypeCode,
    String parentIdentifier
) {}
