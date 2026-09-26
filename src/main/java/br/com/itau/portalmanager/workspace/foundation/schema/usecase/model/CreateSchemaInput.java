package br.com.itau.portalmanager.workspace.foundation.schema.usecase.model;

public record CreateSchemaInput(
        String schemaTypeCode,
        String code,
        String name,
        String description,
        String workspaceIdentifier
) {
}
