package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.onboarding;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.onboarding.OnboardingPhase;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.support.CatalogSettingsSchemaValidator;
import br.com.portalmanager.platform.catalog.mapper.BaseCatalogMapper;
import org.springframework.stereotype.Component;

@Component
public class OnboardingPhaseMapper extends BaseCatalogMapper<OnboardingPhaseDTO, OnboardingPhase> {

    private final CatalogSettingsSchemaValidator settingsValidator;

    public OnboardingPhaseMapper(CatalogSettingsSchemaValidator settingsValidator) {
        super(OnboardingPhase.class);
        this.settingsValidator = settingsValidator;
    }

    @Override
    public OnboardingPhaseDTO toDTO(OnboardingPhase entity) {
        if (entity == null) return null;
        return new OnboardingPhaseDTO(
                entity.getId(), entity.getName(), entity.getLabel(), entity.getDescription(),
                entity.getSortOrder(), entity.getOrientation(),
                settingsValidator.fromString(entity.getSettings())
        );
    }

    @Override
    public OnboardingPhase toEntity(OnboardingPhaseDTO dto) {
        OnboardingPhase entity = super.toEntity(dto);
        mapAdditionalFields(entity, dto);
        return entity;
    }

    @Override
    public void updateEntity(OnboardingPhase entity, OnboardingPhaseDTO dto) {
        super.updateEntity(entity, dto);
        mapAdditionalFields(entity, dto);
    }

    private void mapAdditionalFields(OnboardingPhase entity, OnboardingPhaseDTO dto) {
        entity.setOrientation(dto.orientation() == null ? "" : dto.orientation());
    }
}
