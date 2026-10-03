package br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseType;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.repository.OnboardingPhaseTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class OnboardingPhaseTypeService extends EnumCatalogService<OnboardingPhaseType, OnboardingPhaseTypeEnum> {

    private static final String SCHEMA_RESOURCE_CODE = "onboarding-phase-type";

    public OnboardingPhaseTypeService(
            OnboardingPhaseTypeRepository repository,
            ObjectMapper objectMapper,
            SchemaValidator schemaValidator) {
        super(
                repository,
                objectMapper,
                OnboardingPhaseType.class,
                OnboardingPhaseTypeEnum.class,
                SCHEMA_RESOURCE_CODE,
                schemaValidator
        );
    }
}
