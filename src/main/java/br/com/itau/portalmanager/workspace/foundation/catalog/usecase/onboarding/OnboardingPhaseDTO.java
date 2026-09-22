package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.onboarding;

import br.com.portalmanager.platform.catalog.dto.BaseCatalogDTO;
import tools.jackson.databind.JsonNode;

public record OnboardingPhaseDTO(
        Long id,
        String name,
        String label,
        String description,
        Integer sortOrder,
        String orientation,
        JsonNode settings
) implements BaseCatalogDTO<OnboardingPhaseDTO> {
    @Override
    public OnboardingPhaseDTO withId(Long id) {
        return new OnboardingPhaseDTO(id, name, label, description, sortOrder, orientation, settings);
    }
}
