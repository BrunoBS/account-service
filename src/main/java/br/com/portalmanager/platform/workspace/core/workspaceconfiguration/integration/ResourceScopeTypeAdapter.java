package br.com.portalmanager.platform.workspace.core.workspaceconfiguration.integration;

import br.com.portalmanager.platform.workspace.core.workspaceconfiguration.usecase.port.ResourceScopeTypePort;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.usecase.ResourceScopeTypeService;

import org.springframework.stereotype.Component;

@Component("coreworkspaceconfigurationResourceScopeTypeAdapter")
public class ResourceScopeTypeAdapter implements ResourceScopeTypePort {
    private final ResourceScopeTypeService catalog;

    public ResourceScopeTypeAdapter(ResourceScopeTypeService catalog) {
        this.catalog = catalog;
    }

    @Override
    public boolean existsActive(String code) {
        return catalog.existsActive(code);
    }
}
