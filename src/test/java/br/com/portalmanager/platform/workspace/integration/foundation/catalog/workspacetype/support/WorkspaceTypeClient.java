package br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support;

import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.support.CatalogClient;
import org.springframework.stereotype.Component;

@Component
public final class WorkspaceTypeClient extends CatalogClient {
    public WorkspaceTypeClient(PlatformRequestSpecificationFactory requests) {
        super(requests, "/api/v1/workspace-type");
    }
}
