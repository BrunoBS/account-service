package br.com.portalmanager.platform.workspace.entrypoint.catalog.schema_scope;

import br.com.portalmanager.platform.workspace.entrypoint.catalog.CatalogClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import org.springframework.stereotype.Component;

@Component
public final class SchemaScopeTypeClient extends CatalogClient {
    public SchemaScopeTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/schema-scope");
    }
}
