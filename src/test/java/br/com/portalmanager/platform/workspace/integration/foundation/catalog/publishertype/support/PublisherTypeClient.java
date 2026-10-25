package br.com.portalmanager.platform.workspace.integration.foundation.catalog.publishertype.support;

import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogClient;
import org.springframework.stereotype.Component;

@Component
public final class PublisherTypeClient extends CatalogClient {
    public PublisherTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/publishers");
    }
}
