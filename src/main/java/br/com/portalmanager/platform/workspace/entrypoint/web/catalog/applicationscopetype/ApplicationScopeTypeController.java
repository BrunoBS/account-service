package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.applicationscopetype;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.usecase.ApplicationScopeTypeService;
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
