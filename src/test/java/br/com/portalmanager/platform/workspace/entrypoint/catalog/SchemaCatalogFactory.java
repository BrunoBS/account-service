package br.com.portalmanager.platform.workspace.entrypoint.catalog;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

public final class SchemaCatalogFactory {
    public List<CatalogDTO> scopes() {
        return List.of(
                item("PLATFORM", "Platform", "Platform-owned schemas", 1),
                item("WORKSPACE", "Workspace", "Workspace-owned schemas", 2));
    }

    public List<CatalogDTO> versionStatuses() {
        return List.of(
                item("DRAFT", "Draft", "Draft schema version", 1),
                item("PUBLISHED", "Published", "Published schema version", 2));
    }

    private CatalogDTO item(String code, String label, String description, int order) {
        return new CatalogDTO(code, label, description, order, JsonMapper.builder().build().createObjectNode());
    }
}
