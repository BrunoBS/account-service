package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.version.SchemaVersion;

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
            version.getStatus().toString(),
            version.getDefinition()
        );
    }
}
