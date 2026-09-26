package br.com.itau.portalmanager.workspace.foundation.schema.usecase.model;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.Schema;

public record SchemaOutput(
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
    public static SchemaOutput from(Schema schema) {
        return new SchemaOutput(
                schema.getIdentifier(),
                schema.getVersion(),
                schema.getSchemaTypeCode(),
                schema.getScope().toString(),
                schema.getWorkspaceIdentifier(),
                schema.getCode(),
                schema.getName(),
                schema.getDescription(),
                schema.getLifecycle().toString()
        );
    }
}
