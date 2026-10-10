package br.com.portalmanager.platform.workspace.entrypoint.web.schema.response;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaOutput;

public record SchemaResponse(
    String identifier,
    Long version,
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
            output.scope(),
            output.workspaceIdentifier(),
            output.code(),
            output.name(),
            output.description(),
            output.lifecycle()
        );
    }
}
