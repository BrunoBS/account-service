package br.com.portalmanager.platform.workspace.integration.foundation.catalog.support;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

public final class CatalogBuilder implements TestDataBuilder<CatalogDTO> {
    private String code = "TEST_CATALOG";
    private String label = "Catalog item for tests";
    private String description = "Catalog item created by an integration test";
    private Integer sortOrder = 900;
    private JsonNode settings = JsonMapper.builder().build().createObjectNode();

    public static CatalogBuilder builder() { return new CatalogBuilder(); }

    public CatalogBuilder withCode(String value) { code = value; return this; }
    public CatalogBuilder withLabel(String value) { label = value; return this; }
    public CatalogBuilder withDescription(String value) { description = value; return this; }
    public CatalogBuilder withSortOrder(Integer value) { sortOrder = value; return this; }
    public CatalogBuilder withSettings(JsonNode value) { settings = value; return this; }

    @Override
    public CatalogDTO build() {
        return new CatalogDTO(code, label, description, sortOrder, settings);
    }
}
