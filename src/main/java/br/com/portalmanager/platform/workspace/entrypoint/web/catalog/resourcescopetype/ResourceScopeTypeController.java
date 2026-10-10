package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.resourcescopetype;

import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.facade.ResourceScopeTypeFacade;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resource-scope-type")
public class ResourceScopeTypeController extends CatalogController<ResourceScopeType> {

    public ResourceScopeTypeController(ResourceScopeTypeFacade facade) {
        super(facade);
    }
}
