package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation;

import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.domain.featurecontext.FeatureContext;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.CreateFeatureContextInput;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.model.UpdateFeatureContextInput;
import java.util.List;
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

    public void validateAssociation(FeatureContext context) {
        if (!context.isActive()) {
            PlatformValidation.reject("featureIdentifiers", PlatformMessageKeys.CONTEXT_INACTIVE);
        }
    }

    public void validateFeatureIdentifiers(List<String> identifiers) {
        if (
            identifiers != null &&
            identifiers.stream().anyMatch(identifier -> identifier == null || identifier.isBlank())
        ) {
            PlatformValidation.reject("featureIdentifiers", PlatformMessageKeys.FEATURE_NOT_FOUND);
        }
    }

    public void validateDelete(boolean hasFeatures) {
        if (hasFeatures) {
            PlatformValidation.reject("context", PlatformMessageKeys.CONTEXT_HAS_FEATURES);
        }
    }
}
