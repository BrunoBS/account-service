package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation;

import br.com.portalmanager.platform.workspace.feature.platform.domain.FeatureContext;
import br.com.portalmanager.platform.workspace.feature.platform.domain.Microservice;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureInput;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaResourceType;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaSettingsValidator;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.library.messaging.validation.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FeatureValidator {
    private final SchemaSettingsValidator settingsValidator;

    /** Used by isolated command tests; production always injects the shared validator. */
    public FeatureValidator() { this.settingsValidator = null; }

    @Autowired
    public FeatureValidator(SchemaSettingsValidator settingsValidator) { this.settingsValidator = settingsValidator; }

    public void validateSettings(String featureCode, String settings) {
        if (settingsValidator == null) return;
        ValidationResult result = new ValidationResult();
        settingsValidator.validate(SchemaResourceType.FEATURE, featureCode, settings, result);
        if (result.hasErrors()) throw new ValidationException(result);
    }
    public void validateCreate(CreateFeatureInput input, boolean codeDuplicate, boolean nameDuplicate) {
        PlatformValidation.requireInput(input);
        PlatformValidation.validate(input.code(), true, input.name(), input.description(), codeDuplicate,
                nameDuplicate, input.microserviceIdentifier(), true, input.settings(), true);
    }

    public void validateUpdate(UpdateFeatureInput input, boolean nameDuplicate) {
        PlatformValidation.requireInput(input);
        PlatformValidation.validate(null, false, input.name(), input.description(), false,
                nameDuplicate, input.microserviceIdentifier(), true, input.settings(), true);
    }

    public void validateMicroservice(Microservice microservice) {
        if (microservice == null || !microservice.isActive()) {
            PlatformValidation.reject("microserviceIdentifier", PlatformMessageKeys.MICROSERVICE_INACTIVE);
        }
    }

    public void validateContext(FeatureContext context) {
        if (context == null || !context.isActive()) {
            PlatformValidation.reject("contextIdentifier", PlatformMessageKeys.CONTEXT_INACTIVE);
        }
    }
}
