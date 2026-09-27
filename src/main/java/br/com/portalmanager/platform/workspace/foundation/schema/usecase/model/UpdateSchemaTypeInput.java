package br.com.portalmanager.platform.workspace.foundation.schema.usecase.model;

import java.util.Set;

public record UpdateSchemaTypeInput(
        Long version,
        String name,
        String description,
        Set<String> allowedScopes
) {
}
