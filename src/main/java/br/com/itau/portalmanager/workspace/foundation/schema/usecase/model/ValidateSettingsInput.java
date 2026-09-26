package br.com.itau.portalmanager.workspace.foundation.schema.usecase.model;

import br.com.itau.portalmanager.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeCode;
import tools.jackson.databind.JsonNode;

public record ValidateSettingsInput(
        String schemaTypeCode,
        String scope,
        String workspaceIdentifier,
        String schemaCode,
        JsonNode settings
) {
    public static ValidateSettingsInput platform(String schemaTypeCode, JsonNode settings) {
        return new ValidateSettingsInput(
                schemaTypeCode,
                SchemaScopeTypeCode.platform().value(),
                null,
                null,
                settings
        );
    }

    public static ValidateSettingsInput workspace(
            String workspaceIdentifier,
            String schemaTypeCode,
            String schemaCode,
            JsonNode settings
    ) {
        return new ValidateSettingsInput(
                schemaTypeCode,
                SchemaScopeTypeCode.workspace().value(),
                workspaceIdentifier,
                schemaCode,
                settings
        );
    }
}
