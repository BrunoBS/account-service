package br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.featurecontext;

import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.domain.context.Context;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.validation.PlatformValidation;
import org.springframework.stereotype.Component;

@Component
public class FeatureContextValidator {
    public void validateContextForAssociation(Context context) {
        if (!context.isActive()) {
            PlatformValidation.reject("contextIdentifier", PlatformMessageKeys.CONTEXT_INACTIVE);
        }
    }
}
