package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.schema;

public record SchemaOutput(
    String identifier,
    Long version,
    String scope,
    String workspaceIdentifier,
    String code,
    String name,
    String description,
    String lifecycle
) {}
