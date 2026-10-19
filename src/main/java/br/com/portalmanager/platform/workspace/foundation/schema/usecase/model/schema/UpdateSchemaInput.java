package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.schema;

public record UpdateSchemaInput(Long version, String name, String description) {}
