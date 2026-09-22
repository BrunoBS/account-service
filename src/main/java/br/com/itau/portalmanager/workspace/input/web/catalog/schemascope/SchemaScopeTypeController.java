package br.com.itau.portalmanager.workspace.input.web.catalog.schemascope;

import br.com.itau.portalmanager.workspace.foundation.catalog.schemascope.domain.SchemaScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.schemascope.usecase.SchemaScopeTypeService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
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
