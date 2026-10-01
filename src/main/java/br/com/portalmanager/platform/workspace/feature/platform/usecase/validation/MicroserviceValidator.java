package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation;

import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import br.com.portalmanager.platform.workspace.feature.platform.domain.Microservice;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateMicroserviceInput;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

@Component
public class MicroserviceValidator {
    private final SchemaSettingsValidator settingsValidator;

    public MicroserviceValidator() { this.settingsValidator = null; }
    @Autowired
    public MicroserviceValidator(SchemaSettingsValidator settingsValidator) { this.settingsValidator = settingsValidator; }

    public void validateSettings(String code, JsonNode settings) {
        if (settingsValidator == null) return;
        ValidationResult result = new ValidationResult();
        settingsValidator.validate("MICROSERVICE", code, "settings", settings, result);
        if (result.hasErrors()) throw new ValidationException(result);
    }

    public void validateCreate(CreateMicroserviceInput input, boolean codeDuplicate, boolean nameDuplicate) {
        PlatformValidation.requireInput(input);
        PlatformValidation.validate(input.code(), true, input.name(), input.description(), codeDuplicate,
                nameDuplicate, null, false, input.settings(), true);
    }

    public void validateUpdate(UpdateMicroserviceInput input, boolean nameDuplicate) {
        PlatformValidation.requireInput(input);
        PlatformValidation.validate(null, false, input.name(), input.description(), false,
                nameDuplicate, null, false, input.settings(), true);
    }

    public void validateDelete(Microservice microservice) {
        if (!microservice.getFeatures().isEmpty()) {
            PlatformValidation.reject("microservice", PlatformMessageKeys.MICROSERVICE_HAS_FEATURES);
        }
    }
}
