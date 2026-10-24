package br.com.portalmanager.platform.workspace.entrypoint.catalog.authorization;

import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogClient;
import org.springframework.stereotype.Component;

@Component
public final class AuthorizationTypeClient extends CatalogClient {
    public AuthorizationTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/authorization-type");
    }
}
