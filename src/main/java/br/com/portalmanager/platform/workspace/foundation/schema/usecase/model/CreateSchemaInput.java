package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model;

import tools.jackson.databind.JsonNode;

public record CreateSchemaInput(
    String code,
    String name,
    String description,
    String workspaceIdentifier,
    String versionName,
    JsonNode definition
) {}
