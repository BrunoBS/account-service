package br.com.itau.portalmanager.workspace.entrypoint.web.schema.response;

import br.com.itau.portalmanager.workspace.foundation.schema.usecase.model.SchemaTypeOutput;

public record SchemaTypeResponse(
        String identifier,
        Long version,
        String code,
        String name,
        String description,
        String lifecycle
) {
    public static SchemaTypeResponse from(SchemaTypeOutput output) {
        return new SchemaTypeResponse(
                output.identifier(),
                output.version(),
                output.code(),
                output.name(),
                output.description(),
                output.lifecycle()
        );
    }
}
