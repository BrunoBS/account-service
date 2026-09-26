package br.com.itau.portalmanager.workspace.entrypoint.web.catalog.schematype;

import br.com.itau.portalmanager.workspace.foundation.catalog.schematype.domain.SchemaType;
import br.com.itau.portalmanager.workspace.foundation.catalog.schematype.usecase.SchemaTypeService;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.catalog.web.CatalogController;
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
