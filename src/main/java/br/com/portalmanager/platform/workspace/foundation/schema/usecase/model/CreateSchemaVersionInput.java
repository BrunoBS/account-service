package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model;

import tools.jackson.databind.JsonNode;

public record CreateSchemaVersionInput(String versionName, JsonNode definition) {}
