package br.com.itau.portalmanager.workspace.entrypoint.web.schema.response;

import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.SchemaOutput;

public record SchemaResponse(
        String identifier,
        Long version,
        String schemaTypeCode,
        String scope,
        String workspaceIdentifier,
        String code,
        String name,
        String description,
        String lifecycle
) {
    public static SchemaResponse from(SchemaOutput output) {
        return new SchemaResponse(
                output.identifier(),
                output.version(),
                output.schemaTypeCode(),
                output.scope(),
                output.workspaceIdentifier(),
                output.code(),
                output.name(),
                output.description(),
                output.lifecycle()
        );
    }
}
