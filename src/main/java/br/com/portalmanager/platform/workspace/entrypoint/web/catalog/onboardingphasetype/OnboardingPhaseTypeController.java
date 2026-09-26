package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.onboardingphasetype;

import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseType;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.usecase.OnboardingPhaseTypeService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
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
