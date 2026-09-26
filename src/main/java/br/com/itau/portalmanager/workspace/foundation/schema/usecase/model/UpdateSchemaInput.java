package br.com.itau.portalmanager.workspace.foundation.schema.usecase.model;

public record UpdateSchemaInput(
        Long version,
        String name,
        String description
) {
}
