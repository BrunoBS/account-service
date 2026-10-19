package br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment;

import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

public record EnvironmentOutput(
    Long version,
    String identifier,
    String workspaceIdentifier,
    String name,
    String description,
    String authorizationType,
    String environmentTypeCode,
    String parentIdentifier,
    Integer sortOrder,
    String authorizerGroup,
    JsonNode settings,
    String lifecycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
