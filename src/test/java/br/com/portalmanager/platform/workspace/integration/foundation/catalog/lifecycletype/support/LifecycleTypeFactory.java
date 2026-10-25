package br.com.portalmanager.platform.workspace.integration.foundation.catalog.lifecycletype.support;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogBuilder;

import java.util.List;

public final class LifecycleTypeFactory extends AbstractTestDataFactory<CatalogDTO, CatalogBuilder> {
    @Override
    protected CatalogBuilder builder() {
        return CatalogBuilder.builder();
    }

    public CatalogDTO active() {
        return builder().withCode("ACTIVE").withLabel("Active").withDescription("Active lifecycle")
                .withSortOrder(1).build();
    }

    public CatalogDTO inactive() {
        return builder().withCode("INACTIVE").withLabel("Inactive").withDescription("Inactive lifecycle")
                .withSortOrder(2).build();
    }

    public CatalogDTO quarantined() {
        return builder().withCode("QUARANTINED").withLabel("Quarantined")
                .withDescription("Quarantined lifecycle").withSortOrder(3).build();
    }

    public List<CatalogDTO> all() {
        return List.of(active(), inactive(), quarantined());
    }
}
