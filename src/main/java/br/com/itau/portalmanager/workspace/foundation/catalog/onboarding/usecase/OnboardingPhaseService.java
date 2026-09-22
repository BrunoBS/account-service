package br.com.itau.portalmanager.workspace.foundation.catalog.onboarding.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.onboarding.domain.OnboardingPhase;
import br.com.itau.portalmanager.workspace.foundation.catalog.onboarding.repository.OnboardingPhaseRepository;
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
