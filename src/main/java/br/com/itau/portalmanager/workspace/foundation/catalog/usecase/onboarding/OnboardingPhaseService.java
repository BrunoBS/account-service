package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.onboarding;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.onboarding.OnboardingPhase;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.onboarding.OnboardingPhaseRepository;
import br.com.portalmanager.platform.catalog.service.BaseCatalogService;
import org.springframework.stereotype.Service;

@Service
public class OnboardingPhaseService extends BaseCatalogService<OnboardingPhase, OnboardingPhaseDTO> {

    public OnboardingPhaseService(
            OnboardingPhaseRepository repository,
            OnboardingPhaseMapper mapper,
            OnboardingPhaseValidator validator) {
        super(repository, mapper, validator);
    }
}
