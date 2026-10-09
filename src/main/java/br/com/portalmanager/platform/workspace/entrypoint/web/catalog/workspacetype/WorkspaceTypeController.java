package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.workspacetype;

import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceType;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.facade.WorkspaceTypeFacade;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspace-type")
public class WorkspaceTypeController extends CatalogController<WorkspaceType> {

    public WorkspaceTypeController(WorkspaceTypeFacade facade) {
        super(facade);
    }
}
