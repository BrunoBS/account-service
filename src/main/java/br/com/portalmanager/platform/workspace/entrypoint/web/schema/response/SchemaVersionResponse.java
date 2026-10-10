package br.com.portalmanager.platform.workspace.entrypoint.web.schema.response;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaVersionOutput;

public record SchemaVersionResponse(
    String identifier,
    Integer version,
    String versionName,
    String status,
    String definition
) {
    public static SchemaVersionResponse from(SchemaVersionOutput output) {
        return new SchemaVersionResponse(
            output.identifier(),
            output.version(),
            output.versionName(),
            output.status(),
            output.definition()
        );
    }
}
