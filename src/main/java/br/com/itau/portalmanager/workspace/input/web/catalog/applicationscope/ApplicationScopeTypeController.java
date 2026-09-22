package br.com.itau.portalmanager.workspace.input.web.catalog.applicationscope;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.applicationscope.ApplicationScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.applicationscope.ApplicationScopeTypeService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/application-scope-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class ApplicationScopeTypeController extends CatalogController<ApplicationScopeType> {

    public ApplicationScopeTypeController(ApplicationScopeTypeService service) {
        super(service);
    }
}
