package br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSettingsValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseType;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.repository.OnboardingPhaseTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class OnboardingPhaseTypeService extends EnumCatalogService<OnboardingPhaseType, OnboardingPhaseTypeEnum> {

    private static final String SCHEMA_TYPE_CODE = "ONBOARDING_PHASE_TYPE";

    public OnboardingPhaseTypeService(
            OnboardingPhaseTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                OnboardingPhaseType.class,
                OnboardingPhaseTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(SCHEMA_TYPE_CODE, dto.settings(), result)
        );
    }
}
