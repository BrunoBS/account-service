package br.com.itau.portalmanager.workspace.foundation.schema.usecase;

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
