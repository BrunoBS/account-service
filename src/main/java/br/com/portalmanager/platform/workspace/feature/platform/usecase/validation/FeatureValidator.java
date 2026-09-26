package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureInput;
import br.com.portalmanager.platform.workspace.feature.platform.domain.Service;
import br.com.portalmanager.platform.workspace.feature.platform.domain.FeatureContext;
import org.springframework.stereotype.Component;

@Component
public class FeatureValidator {
    public void validateCreate(CreateFeatureInput input, boolean codeDuplicate, boolean nameDuplicate) {
        PlatformValidation.requireInput(input);
        PlatformValidation.validate(input.code(), true, input.name(), input.description(), codeDuplicate,
                nameDuplicate, input.serviceIdentifier(), true, input.settings(), true);
    }

    public void validateUpdate(UpdateFeatureInput input, boolean nameDuplicate) {
        PlatformValidation.requireInput(input);
        PlatformValidation.validate(null, false, input.name(), input.description(), false,
                nameDuplicate, input.serviceIdentifier(), true, input.settings(), true);
    }

    public void validateService(Service service) {
        if (service == null || !service.isActive()) {
            PlatformValidation.reject("serviceIdentifier", "service.inactive");
        }
    }

    public void validateContext(FeatureContext context) {
        if (context == null || !context.isActive()) {
            PlatformValidation.reject("contextIdentifier", "context.inactive");
        }
    }
}
