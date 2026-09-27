package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model;

import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaType;

import java.util.Set;
import java.util.stream.Collectors;

public record SchemaTypeOutput(
        String identifier,
        Long version,
        String code,
        String name,
        String description,
        String lifecycle,
        Set<String> allowedScopes
) {
    public static SchemaTypeOutput from(SchemaType schemaType) {
        return new SchemaTypeOutput(
                schemaType.getIdentifier(),
                schemaType.getVersion(),
                schemaType.getCode(),
                schemaType.getName(),
                schemaType.getDescription(),
                schemaType.getLifecycle().toString(),
                schemaType.getAllowedScopes().stream()
                        .map(Object::toString)
                        .collect(Collectors.toUnmodifiableSet())
        );
    }
}
