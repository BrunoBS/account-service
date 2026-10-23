package br.com.portalmanager.platform.workspace.core.workspace.integration;

import br.com.portalmanager.platform.workspace.core.workspace.usecase.port.WorkspaceTypePort;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.usecase.WorkspaceTypeService;

import org.springframework.stereotype.Component;

@Component
public class WorkspaceTypeAdapter implements WorkspaceTypePort {
    private final WorkspaceTypeService catalog;

    public WorkspaceTypeAdapter(WorkspaceTypeService catalog) {
        this.catalog = catalog;
    }

    @Override
    public boolean existsActive(String code) {
        return catalog.existsActive(code);
    }
}
