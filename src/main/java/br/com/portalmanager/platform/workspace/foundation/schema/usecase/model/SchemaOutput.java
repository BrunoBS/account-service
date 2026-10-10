package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.Schema;

public record SchemaOutput(
    String identifier,
    Long version,
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
            schema.getScope().toString(),
            workspaceIdentifier,
            schema.getCode(),
            schema.getName(),
            schema.getDescription(),
            schema.getLifecycle().toString()
        );
    }
}
