package br.com.portalmanager.platform.workspace.integration.foundation.catalog.resourcescopetype.support;

import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogClient;
import org.springframework.stereotype.Component;

@Component
public final class ResourceScopeTypeClient extends CatalogClient {
    public ResourceScopeTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/resource-scope-type");
    }
}
