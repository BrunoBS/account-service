package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.schematype;

import br.com.portalmanager.platform.workspace.foundation.catalog.schematype.domain.SchemaType;
import br.com.portalmanager.platform.workspace.foundation.catalog.schematype.usecase.SchemaTypeService;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schema-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class SchemaTypeController extends CatalogController<SchemaType> {

    public SchemaTypeController(SchemaTypeService service) {
        super(service);
    }
}
