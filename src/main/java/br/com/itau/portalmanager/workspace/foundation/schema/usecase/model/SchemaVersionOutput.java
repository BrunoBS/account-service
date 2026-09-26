package br.com.itau.portalmanager.workspace.foundation.schema.usecase.model;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaVersion;

public record SchemaVersionOutput(
        String identifier,
        Integer version,
        String versionName,
        String status,
        String definition
) {
    public static SchemaVersionOutput from(SchemaVersion version) {
        return new SchemaVersionOutput(
                version.getIdentifier(),
                version.getSchemaVersion(),
                version.getVersionName(),
                version.getStatus().name(),
                version.getDefinition()
        );
    }
}
