package br.com.portalmanager.platform.workspace.integration.foundation.catalog.schemaversionstatustype.support;

import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import org.springframework.stereotype.Component;

@Component
public final class SchemaVersionStatusTypeClient extends CatalogClient {
    public SchemaVersionStatusTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/schema-version-status-type");
    }
}
