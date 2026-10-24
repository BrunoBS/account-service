package br.com.portalmanager.platform.workspace.entrypoint.catalog.workspace;

import br.com.portalmanager.platform.library.catalog.dto.CatalogDTO;
import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogBuilder;

import java.util.List;

public final class WorkspaceTypeFactory extends AbstractTestDataFactory<CatalogDTO, CatalogBuilder> {
    @Override
    protected CatalogBuilder builder() {
        return CatalogBuilder.builder();
    }

    public CatalogDTO admin() {
        return builder().withCode("ADMIN").withLabel("Admin").withDescription("Workspace administrado")
                .withSortOrder(1).build();
    }

    public CatalogDTO manager() {
        return builder().withCode("MANAGER").withLabel("Manager").withDescription("Workspace gerenciado")
                .withSortOrder(2).build();
    }

    public List<CatalogDTO> all() {
        return List.of(admin(), manager());
    }
}
