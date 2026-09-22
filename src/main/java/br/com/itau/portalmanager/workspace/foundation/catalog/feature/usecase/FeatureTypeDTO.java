package br.com.itau.portalmanager.workspace.foundation.catalog.feature.usecase;

import br.com.portalmanager.platform.catalog.dto.BaseCatalogDTO;
import tools.jackson.databind.JsonNode;

public record FeatureTypeDTO(
        Long id,
        String name,
        String label,
        String description,
        Integer sortOrder,
        JsonNode settings,
        Long featureScopeId,
        String featureScopeName,
        Boolean available
) implements BaseCatalogDTO<FeatureTypeDTO> {
    @Override
    public FeatureTypeDTO withId(Long id) {
        return new FeatureTypeDTO(id, name, label, description, sortOrder, settings,
                featureScopeId, featureScopeName, available);
    }
}
