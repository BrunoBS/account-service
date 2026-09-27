package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model;

import java.util.Set;

public record CreateSchemaTypeInput(
        String code,
        String name,
        String description,
        Set<String> allowedScopes
) {
}
