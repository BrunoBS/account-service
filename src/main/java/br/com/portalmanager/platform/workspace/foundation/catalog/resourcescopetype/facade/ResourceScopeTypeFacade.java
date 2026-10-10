package br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.usecase.ResourceScopeTypeService;
import org.springframework.stereotype.Component;

@Component
public class ResourceScopeTypeFacade extends AbstractCatalogFacade<ResourceScopeType> {

    public ResourceScopeTypeFacade(ResourceScopeTypeService service) {
        super(service);
    }
}
