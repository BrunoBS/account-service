package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.schemaversionstatustype;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusType;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.usecase.SchemaVersionStatusTypeService;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schema-version-status-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class SchemaVersionStatusTypeController extends CatalogController<SchemaVersionStatusType> {

    public SchemaVersionStatusTypeController(SchemaVersionStatusTypeService service) {
        super(service);
    }
}
