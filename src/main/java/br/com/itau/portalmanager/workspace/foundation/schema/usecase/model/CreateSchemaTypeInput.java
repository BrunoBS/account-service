package br.com.itau.portalmanager.workspace.foundation.schema.usecase.model;

public record CreateSchemaTypeInput(
        String code,
        String name,
        String description
) {
}
