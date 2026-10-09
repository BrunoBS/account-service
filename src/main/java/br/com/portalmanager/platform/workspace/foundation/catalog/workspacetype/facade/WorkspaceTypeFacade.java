package br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.usecase.WorkspaceTypeService;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceType;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceTypeFacade extends AbstractCatalogFacade<WorkspaceType> {
    public WorkspaceTypeFacade(WorkspaceTypeService service) {
        super(service);
    }
}
