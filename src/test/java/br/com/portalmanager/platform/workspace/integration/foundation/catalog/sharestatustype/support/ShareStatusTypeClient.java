package br.com.portalmanager.platform.workspace.integration.foundation.catalog.sharestatustype.support;

import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogClient;
import org.springframework.stereotype.Component;

@Component
public final class ShareStatusTypeClient extends CatalogClient {
    public ShareStatusTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/share-status-type");
    }
}
