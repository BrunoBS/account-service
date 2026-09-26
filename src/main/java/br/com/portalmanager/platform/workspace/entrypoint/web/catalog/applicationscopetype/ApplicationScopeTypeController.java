package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.applicationscopetype;

import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.usecase.ApplicationScopeTypeService;
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
