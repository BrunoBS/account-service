package br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.usecase;

import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseType;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.repository.OnboardingPhaseTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class OnboardingPhaseTypeService extends EnumCatalogService<OnboardingPhaseType, OnboardingPhaseTypeEnum> {

    public OnboardingPhaseTypeService(
            OnboardingPhaseTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                OnboardingPhaseType.class,
                OnboardingPhaseTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
