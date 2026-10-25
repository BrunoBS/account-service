package br.com.portalmanager.platform.workspace.integration.foundation.catalog.tagorigintype.support;

import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogClient;
import org.springframework.stereotype.Component;

@Component
public final class TagOriginTypeClient extends CatalogClient {
    public TagOriginTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/tag-origin-type");
    }
}
