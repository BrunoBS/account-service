package br.com.portalmanager.platform.workspace.integration.foundation.catalog.visibilitytype.support;

import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogClient;
import org.springframework.stereotype.Component;

@Component
public final class VisibilityTypeClient extends CatalogClient {
    public VisibilityTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/visibility-type");
    }
}
