package br.com.portalmanager.platform.workspace.integration.foundation.catalog.publicationmodetype.support;

import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogClient;
import org.springframework.stereotype.Component;

@Component
public final class PublicationModeTypeClient extends CatalogClient {
    public PublicationModeTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/publication-mode-type");
    }
}
