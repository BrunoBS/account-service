package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.resourcescopetype;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.usecase.ResourceScopeTypeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resource-scope-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class ResourceScopeTypeController extends CatalogController<ResourceScopeType> {

    public ResourceScopeTypeController(ResourceScopeTypeService service) {
        super(service);
    }
}
