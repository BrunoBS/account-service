package br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusType;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.usecase.SchemaVersionStatusTypeService;
import org.springframework.stereotype.Component;

@Component
public class SchemaVersionStatusTypeFacade extends AbstractCatalogFacade<SchemaVersionStatusType> {

    public SchemaVersionStatusTypeFacade(SchemaVersionStatusTypeService service) {
        super(service);
    }
}
