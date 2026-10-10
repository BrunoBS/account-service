package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation;

import br.com.portalmanager.platform.workspace.feature.platform.domain.FeatureContext;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureContextInput;
import org.springframework.stereotype.Component;

@Component
public class FeatureContextValidator {

    public void validateCreate(CreateFeatureContextInput input, boolean codeDuplicate, boolean nameDuplicate) {
        PlatformValidation.requireInput(input);
        PlatformValidation.validate(
            input.code(),
            true,
            input.name(),
            input.description(),
            codeDuplicate,
            nameDuplicate,
            null,
            false,
            null,
            false
        );
    }

    public void validateUpdate(UpdateFeatureContextInput input, boolean nameDuplicate) {
        PlatformValidation.requireInput(input);
        PlatformValidation.validate(
            null,
            false,
            input.name(),
            input.description(),
            false,
            nameDuplicate,
            null,
            false,
            null,
            false
        );
    }

    public void validateDelete(FeatureContext context) {
        if (!context.getFeatures().isEmpty()) {
            PlatformValidation.reject("context", PlatformMessageKeys.CONTEXT_HAS_FEATURES);
        }
    }
}
