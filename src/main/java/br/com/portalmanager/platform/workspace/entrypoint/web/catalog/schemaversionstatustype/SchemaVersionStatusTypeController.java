package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.schemaversionstatustype;

import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusType;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.facade.SchemaVersionStatusTypeFacade;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schema-version-status-type")
public class SchemaVersionStatusTypeController extends CatalogController<SchemaVersionStatusType> {

    public SchemaVersionStatusTypeController(SchemaVersionStatusTypeFacade facade) {
        super(facade);
    }
}
