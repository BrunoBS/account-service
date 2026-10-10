package br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseType;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.usecase.OnboardingPhaseTypeService;
import org.springframework.stereotype.Component;

@Component
public class OnboardingPhaseTypeFacade extends AbstractCatalogFacade<OnboardingPhaseType> {

    public OnboardingPhaseTypeFacade(OnboardingPhaseTypeService service) {
        super(service);
    }
}
