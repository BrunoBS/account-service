package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.version;

import tools.jackson.databind.JsonNode;

public record CreateSchemaVersionInput(String versionName, JsonNode definition) {}
