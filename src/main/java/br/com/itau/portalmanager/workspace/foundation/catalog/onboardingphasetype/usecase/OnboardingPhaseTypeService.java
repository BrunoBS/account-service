package br.com.itau.portalmanager.workspace.foundation.catalog.onboardingphasetype.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseType;
import br.com.itau.portalmanager.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.onboardingphasetype.repository.OnboardingPhaseTypeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class OnboardingPhaseTypeTypeService extends EnumCatalogService<OnboardingPhaseType, OnboardingPhaseTypeTypeEnum> {

    public OnboardingPhaseTypeTypeService(
            OnboardingPhaseTypeTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                OnboardingPhaseType.class,
                OnboardingPhaseTypeTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
