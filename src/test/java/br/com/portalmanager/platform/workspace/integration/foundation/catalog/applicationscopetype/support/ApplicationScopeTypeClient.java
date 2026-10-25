package br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support;

import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import org.springframework.stereotype.Component;

@Component
public final class ApplicationScopeTypeClient extends CatalogClient {
    public ApplicationScopeTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/application-scope-type");
    }
}
