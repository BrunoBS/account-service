package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation;

import br.com.portalmanager.platform.workspace.feature.platform.domain.FeatureContext;
import br.com.portalmanager.platform.workspace.feature.platform.domain.Microservice;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureInput;
import org.springframework.stereotype.Component;

@Component
public class FeatureValidator {
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
            PlatformValidation.reject("microserviceIdentifier", "microservice.inactive");
        }
    }

    public void validateContext(FeatureContext context) {
        if (context == null || !context.isActive()) {
            PlatformValidation.reject("contextIdentifier", "context.inactive");
        }
    }
}
