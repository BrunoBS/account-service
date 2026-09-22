package br.com.itau.portalmanager.workspace.input.web.catalog.onboarding;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.onboarding.OnboardingPhase;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.onboarding.OnboardingPhaseService;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.onboarding.OnboardingPhaseDTO;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerBaseCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding-type")
public class OnboardingPhaseController extends OwnerBaseCatalogController<OnboardingPhaseDTO, OnboardingPhase> {

    private final OnboardingPhaseService service;

    public OnboardingPhaseController(OnboardingPhaseService service) {
        this.service = service;
    }

    @Override
    protected OnboardingPhaseService getService() {
        return service;
    }
}
