package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.resourcescopetype;

import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.usecase.ResourceScopeTypeService;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.catalog.web.CatalogController;
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
