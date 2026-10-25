package br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogBuilder;

public final class CatalogFactory extends AbstractTestDataFactory<CatalogDTO, CatalogBuilder> {
    @Override
    protected CatalogBuilder builder() {
        return CatalogBuilder.builder();
    }

    public CatalogDTO valid(String code) {
        return builder().withCode(code).build();
    }

    public CatalogDTO withInvalidCode() {
        return builder().withCode("NOT_A_CATALOG_ENUM").build();
    }

    public CatalogDTO withoutLabel(String code) {
        return builder().withCode(code).withLabel(null).build();
    }

    public CatalogDTO withLabel(String code, String label) {
        return builder().withCode(code).withLabel(label).build();
    }
}
