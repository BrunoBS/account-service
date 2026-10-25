package br.com.portalmanager.platform.workspace.integration.foundation.catalog.lifecycletype.support;

import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogClient;
import org.springframework.stereotype.Component;

@Component
public final class LifecycleTypeClient extends CatalogClient {
    public LifecycleTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/lifecycle-type");
    }
}
