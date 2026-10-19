package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.version;

public record SchemaVersionOutput(
    String identifier,
    Integer version,
    String versionName,
    String status,
    String definition
) {}
