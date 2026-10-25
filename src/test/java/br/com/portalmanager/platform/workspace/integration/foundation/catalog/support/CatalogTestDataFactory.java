package br.com.portalmanager.platform.workspace.integration.foundation.catalog.support;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;

public final class CatalogTestDataFactory extends AbstractTestDataFactory<CatalogDTO, CatalogBuilder> {
    @Override
    protected CatalogBuilder builder() {
        return CatalogBuilder.builder();
    }

    public CatalogDTO valid(String code) {
        return builder().withCode(code).build();
    }

    public CatalogDTO withLabel(String code, String label) {
        return builder().withCode(code).withLabel(label).build();
    }

    public CatalogDTO withoutLabel(String code) {
        return builder().withCode(code).withLabel(null).build();
    }

    public CatalogDTO withInvalidCode() {
        return builder().withCode("NOT_A_CATALOG_ENUM").build();
    }
}
