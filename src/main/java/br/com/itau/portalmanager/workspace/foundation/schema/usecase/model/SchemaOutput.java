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
    public static SchemaOutput from(Schema schema, String workspaceIdentifier) {
        return new SchemaOutput(
                schema.getIdentifier(),
                schema.getVersion(),
                schema.getSchemaTypeCode(),
                schema.getScope().toString(),
                workspaceIdentifier,
                schema.getCode(),
                schema.getName(),
                schema.getDescription(),
                schema.getLifecycle().toString()
        );
    }
}
