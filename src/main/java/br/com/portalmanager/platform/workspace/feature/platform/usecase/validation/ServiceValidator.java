package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation;

import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateServiceInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateServiceInput;
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
}
