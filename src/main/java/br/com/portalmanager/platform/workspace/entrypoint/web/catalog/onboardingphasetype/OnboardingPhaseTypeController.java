package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.onboardingphasetype;

import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseType;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.facade.OnboardingPhaseTypeFacade;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding-type")
public class OnboardingPhaseTypeController extends CatalogController<OnboardingPhaseType> {

    public OnboardingPhaseTypeController(OnboardingPhaseTypeFacade facade) {
        super(facade);
    }
}
