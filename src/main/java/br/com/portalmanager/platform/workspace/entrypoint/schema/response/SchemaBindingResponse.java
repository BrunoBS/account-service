package br.com.portalmanager.platform.workspace.entrypoint.schema.response;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.binding.SchemaBindingOutput;

public record SchemaBindingResponse(
    String identifier,
    Long version,
    String resourceType,
    String resourceCode,
    String schemaIdentifier,
    String lifecycle
) {
    public static SchemaBindingResponse from(SchemaBindingOutput output) {
        return new SchemaBindingResponse(
            output.identifier(),
            output.version(),
            output.resourceType(),
            output.resourceCode(),
            output.schemaIdentifier(),
            output.lifecycle()
        );
    }
}
