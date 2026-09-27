package br.com.portalmanager.platform.workspace.entrypoint.web.schema.response;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaTypeOutput;

public record SchemaTypeResponse(
        String identifier,
        Long version,
        String code,
        String name,
        String description,
        String lifecycle,
        String scope
) {
    public static SchemaTypeResponse from(SchemaTypeOutput output) {
        return new SchemaTypeResponse(
                output.identifier(),
                output.version(),
                output.code(),
                output.name(),
                output.description(),
                output.lifecycle(),
                output.scope()
        );
    }
}
