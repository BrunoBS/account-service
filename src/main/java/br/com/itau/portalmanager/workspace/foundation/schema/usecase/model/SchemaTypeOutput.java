package br.com.itau.portalmanager.workspace.foundation.schema.usecase.model;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaType;

public record SchemaTypeOutput(
        String identifier,
        Long version,
        String code,
        String name,
        String description,
        String lifecycle
) {
    public static SchemaTypeOutput from(SchemaType type) {
        return new SchemaTypeOutput(
                type.getIdentifier(),
                type.getVersion(),
                type.getCode(),
                type.getName(),
                type.getDescription(),
                type.getLifecycle().toString()
        );
    }
}
