package br.com.itau.portalmanager.workspace.foundation.schema.usecase.model;

import tools.jackson.databind.JsonNode;

public record CreateSchemaInput(
        String schemaTypeCode,
        String code,
        String name,
        String description,
        String workspaceIdentifier,
        String versionName,
        JsonNode definition
) {
}
