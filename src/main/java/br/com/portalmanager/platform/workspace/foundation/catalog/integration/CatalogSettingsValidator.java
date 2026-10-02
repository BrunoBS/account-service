package br.com.portalmanager.platform.workspace.foundation.catalog.integration;

import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class CatalogSettingsValidator {

    private static final String SETTINGS = "settings";
    private static final String RESOURCE_TYPE = "CATALOG";

    private final SchemaSettingsValidator settingsValidator;

    public CatalogSettingsValidator(SchemaSettingsValidator settingsValidator) {
        this.settingsValidator = settingsValidator;
    }

    public void validateSettings(
            String catalogCode,
            JsonNode settings,
            ValidationResult result
    ) {
        settingsValidator.validate(RESOURCE_TYPE, catalogCode, SETTINGS, settings, result);
    }
}
