package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.onboarding;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.onboarding.OnboardingPhaseEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.onboarding.OnboardingPhaseRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.support.CatalogSettingsSchemaValidator;
import br.com.portalmanager.platform.catalog.validation.CatalogValidationResult;
import br.com.portalmanager.platform.catalog.validation.EnumCatalogValidator;
import org.springframework.stereotype.Component;

@Component
public class OnboardingPhaseValidator
        extends EnumCatalogValidator<OnboardingPhaseEnum, OnboardingPhaseDTO> {

    private final CatalogSettingsSchemaValidator settingsValidator;

    public OnboardingPhaseValidator(
            OnboardingPhaseRepository repository,
            CatalogSettingsSchemaValidator settingsValidator) {
        super(repository, OnboardingPhaseEnum.class);
        this.settingsValidator = settingsValidator;
    }

    @Override
    protected void validateSettings(OnboardingPhaseDTO dto, CatalogValidationResult result) {
        settingsValidator.validate(dto.settings(), "settings", result);
    }

    @Override public String entityName() { return "OnboardingPhase"; }
}
