package br.com.itau.portalmanager.workspace.input.web.catalog.schemascope;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.schemascope.SchemaScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.schemascope.SchemaScopeTypeService;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schema-scope")
public class SchemaScopeTypeController extends OwnerCatalogController<SchemaScopeType> {

    public SchemaScopeTypeController(SchemaScopeTypeService service) {
        super(service);
    }
}
