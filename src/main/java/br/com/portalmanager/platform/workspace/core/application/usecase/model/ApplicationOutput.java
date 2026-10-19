package br.com.portalmanager.platform.workspace.core.application.usecase.model;

import java.time.LocalDateTime;
import tools.jackson.databind.JsonNode;

public record ApplicationOutput(
    Long version,
    String identifier,
    String workspaceIdentifier,
    String name,
    String alias,
    String description,
    String acronym,
    String applicationScope,
    String authorizerGroup,
    JsonNode settings,
    String lifecycle,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    java.util.List<String> tags
) {}
