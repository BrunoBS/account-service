package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.schemascopetype;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.usecase.SchemaScopeTypeService;
import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schema-scope")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class SchemaScopeTypeController extends CatalogController<SchemaScopeType> {

    public SchemaScopeTypeController(SchemaScopeTypeService service) {
        super(service);
    }
}
