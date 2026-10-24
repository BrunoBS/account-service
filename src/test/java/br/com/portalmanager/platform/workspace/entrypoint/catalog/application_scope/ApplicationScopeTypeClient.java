package br.com.portalmanager.platform.workspace.entrypoint.catalog.application_scope;

import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import org.springframework.stereotype.Component;

@Component
public final class ApplicationScopeTypeClient extends CatalogClient {
    public ApplicationScopeTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/application-scope-type");
    }
}
