package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model;

public record SchemaResolution(
        String requestedSchemaType,
        String resolvedSchemaType,
        String schemaIdentifier,
        String schemaVersionIdentifier,
        Integer schemaVersion,
        String definition,
        boolean fallback
) {
}
