package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model;

public record CreateSchemaTypeInput(
        String code,
        String name,
        String description,
        String scope
) {
}
