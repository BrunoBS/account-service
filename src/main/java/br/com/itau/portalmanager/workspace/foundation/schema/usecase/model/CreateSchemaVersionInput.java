package br.com.itau.portalmanager.workspace.foundation.schema.usecase.model;

import tools.jackson.databind.JsonNode;

public record CreateSchemaVersionInput(
        String versionName,
        JsonNode definition
) {
}
