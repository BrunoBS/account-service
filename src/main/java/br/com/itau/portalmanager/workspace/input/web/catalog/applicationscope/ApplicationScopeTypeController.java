package br.com.itau.portalmanager.workspace.input.web.catalog.applicationscope;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.applicationscope.ApplicationScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.applicationscope.ApplicationScopeTypeService;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/application-scope-type")
public class ApplicationScopeTypeController extends OwnerCatalogController<ApplicationScopeType> {

    public ApplicationScopeTypeController(ApplicationScopeTypeService service) {
        super(service);
    }
}
