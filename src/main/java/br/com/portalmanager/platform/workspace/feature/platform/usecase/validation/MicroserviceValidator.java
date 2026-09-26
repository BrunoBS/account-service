package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation;

import br.com.portalmanager.platform.workspace.feature.platform.domain.Microservice;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateMicroserviceInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateMicroserviceInput;
import org.springframework.stereotype.Component;

@Component
public class MicroserviceValidator {
    public void validateCreate(CreateMicroserviceInput input, boolean codeDuplicate, boolean nameDuplicate) {
        PlatformValidation.requireInput(input);
        PlatformValidation.validate(input.code(), true, input.name(), input.description(), codeDuplicate,
                nameDuplicate, null, false, null, false);
    }

    public void validateUpdate(UpdateMicroserviceInput input, boolean nameDuplicate) {
        PlatformValidation.requireInput(input);
        PlatformValidation.validate(null, false, input.name(), input.description(), false,
                nameDuplicate, null, false, null, false);
    }

    public void validateDelete(Microservice microservice) {
        if (!microservice.getFeatures().isEmpty()) {
            PlatformValidation.reject("microservice", "microservice.has-features");
        }
    }
}
