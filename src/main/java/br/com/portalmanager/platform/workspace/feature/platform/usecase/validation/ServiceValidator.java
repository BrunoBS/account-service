package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateServiceInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateServiceInput;
import br.com.portalmanager.platform.workspace.feature.platform.domain.Service;
import org.springframework.stereotype.Component;

@Component
public class ServiceValidator {
    public void validateCreate(CreateServiceInput input, boolean codeDuplicate, boolean nameDuplicate) {
        PlatformValidation.requireInput(input);
        PlatformValidation.validate(input.code(), true, input.name(), input.description(), codeDuplicate,
                nameDuplicate, null, false, null, false);
    }

    public void validateUpdate(UpdateServiceInput input, boolean nameDuplicate) {
        PlatformValidation.requireInput(input);
        PlatformValidation.validate(null, false, input.name(), input.description(), false,
                nameDuplicate, null, false, null, false);
    }

    public void validateDelete(Service service) {
        if (!service.getFeatures().isEmpty()) {
            PlatformValidation.reject("service", "service.has-features");
        }
    }
}
