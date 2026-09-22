package br.com.itau.portalmanager.workspace.input.web.catalog.onboarding;

import br.com.itau.portalmanager.workspace.foundation.catalog.onboarding.domain.OnboardingPhase;
import br.com.itau.portalmanager.workspace.foundation.catalog.onboarding.usecase.OnboardingPhaseService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class OnboardingPhaseController extends CatalogController<OnboardingPhase> {

    public OnboardingPhaseController(OnboardingPhaseService service) {
        super(service);
    }
}
