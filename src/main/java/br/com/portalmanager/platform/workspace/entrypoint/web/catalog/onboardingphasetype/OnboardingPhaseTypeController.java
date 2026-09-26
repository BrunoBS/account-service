package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.onboardingphasetype;

import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseType;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.usecase.OnboardingPhaseTypeService;
import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class OnboardingPhaseTypeController extends CatalogController<OnboardingPhaseType> {

    public OnboardingPhaseTypeController(OnboardingPhaseTypeService service) {
        super(service);
    }
}
