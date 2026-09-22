package br.com.itau.portalmanager.workspace.input.web.catalog.onboarding;

import br.com.itau.portalmanager.workspace.foundation.catalog.onboarding.domain.OnboardingPhase;
import br.com.itau.portalmanager.workspace.foundation.catalog.onboarding.usecase.OnboardingPhaseService;
import br.com.itau.portalmanager.workspace.foundation.catalog.onboarding.usecase.OnboardingPhaseDTO;
import br.com.portalmanager.platform.catalog.web.BaseCatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class OnboardingPhaseController extends BaseCatalogController<OnboardingPhaseDTO, OnboardingPhase> {

    private final OnboardingPhaseService service;

    public OnboardingPhaseController(OnboardingPhaseService service) {
        this.service = service;
    }

    @Override
    protected OnboardingPhaseService getService() {
        return service;
    }
}
