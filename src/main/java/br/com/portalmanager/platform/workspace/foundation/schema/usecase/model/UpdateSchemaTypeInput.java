package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model;

public record UpdateSchemaTypeInput(
        Long version,
        String name,
        String description,
        String scope
) {
}
