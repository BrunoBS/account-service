package br.com.portalmanager.platform.workspace.entrypoint.web.schema.response;

import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaConfigurationOutput;

public record SchemaConfigurationResponse(
    String identifier,
    Long version,
    String resourceType,
    String resourceCode,
    String schemaIdentifier,
    String lifecycle
) {
    public static SchemaConfigurationResponse from(SchemaConfigurationOutput output) {
        return new SchemaConfigurationResponse(
            output.identifier(),
            output.version(),
            output.resourceType(),
            output.resourceCode(),
            output.schemaIdentifier(),
            output.lifecycle()
        );
    }
}
