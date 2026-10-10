package br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.usecase.SchemaScopeTypeService;
import org.springframework.stereotype.Component;

@Component
public class SchemaScopeTypeFacade extends AbstractCatalogFacade<SchemaScopeType> {

    public SchemaScopeTypeFacade(SchemaScopeTypeService service) {
        super(service);
    }
}
