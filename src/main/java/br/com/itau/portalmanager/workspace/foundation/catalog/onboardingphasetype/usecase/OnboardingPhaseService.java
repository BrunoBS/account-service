package br.com.itau.portalmanager.workspace.foundation.catalog.onboardingphasetype.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhase;
import br.com.itau.portalmanager.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.onboardingphasetype.repository.OnboardingPhaseRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class OnboardingPhaseService extends EnumCatalogService<OnboardingPhase, OnboardingPhaseEnum> {

    public OnboardingPhaseService(
            OnboardingPhaseRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                OnboardingPhase.class,
                OnboardingPhaseEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
