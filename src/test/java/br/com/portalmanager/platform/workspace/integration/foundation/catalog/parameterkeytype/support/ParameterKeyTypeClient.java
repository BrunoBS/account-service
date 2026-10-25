package br.com.portalmanager.platform.workspace.integration.foundation.catalog.parameterkeytype.support;

import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogClient;
import org.springframework.stereotype.Component;

@Component
public final class ParameterKeyTypeClient extends CatalogClient {
    public ParameterKeyTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/parameter-key-type");
    }
}
