package br.com.portalmanager.platform.workspace.feature.onboarding.integration;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.workspace.feature.onboarding.usecase.port.OnboardingPhaseTypePort;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.usecase.OnboardingPhaseTypeService;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OnboardingPhaseTypeAdapter implements OnboardingPhaseTypePort {
    private final OnboardingPhaseTypeService catalog;

    public OnboardingPhaseTypeAdapter(OnboardingPhaseTypeService catalog) {
        this.catalog = catalog;
    }

    @Override
    public boolean existsActive(String code) {
        return catalog.existsActive(code);
    }

    @Override
    public List<CatalogDTO> findByCodes(List<String> codes) {
        return catalog.findByCodes(codes);
    }
}
