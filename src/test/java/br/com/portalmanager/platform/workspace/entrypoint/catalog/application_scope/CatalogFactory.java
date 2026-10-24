package br.com.portalmanager.platform.workspace.entrypoint.catalog.application_scope;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogBuilder;

public final class CatalogFactory extends AbstractTestDataFactory<CatalogDTO, CatalogBuilder> {
    @Override
    protected CatalogBuilder builder() {
        return CatalogBuilder.builder();
    }

    public CatalogDTO valid(String code) {
        return builder().withCode(code).build();
    }

    public CatalogDTO withoutLabel(String code) {
        return builder().withCode(code).withLabel(null).build();
    }

    public CatalogDTO withLabel(String code, String label) {
        return builder().withCode(code).withLabel(label).build();
    }
}
