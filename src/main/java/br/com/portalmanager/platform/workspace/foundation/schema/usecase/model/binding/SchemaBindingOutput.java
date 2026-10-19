package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.binding;

public record SchemaBindingOutput(
    String identifier,
    Long version,
    String resourceType,
    String resourceCode,
    String schemaIdentifier,
    String lifecycle
) {}
