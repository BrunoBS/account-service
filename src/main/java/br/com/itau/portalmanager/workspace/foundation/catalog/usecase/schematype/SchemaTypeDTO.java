package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.schematype;

import br.com.portalmanager.platform.catalog.dto.BaseCatalogDTO;
import tools.jackson.databind.JsonNode;

public record SchemaTypeDTO(
        Long id,
        String name,
        String label,
        String description,
        Integer sortOrder,
        String scope,
        JsonNode settings
) implements BaseCatalogDTO<SchemaTypeDTO> {
    @Override
    public SchemaTypeDTO withId(Long id) {
        return new SchemaTypeDTO(id, name, label, description, sortOrder, scope, settings);
    }
}
