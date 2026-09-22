package br.com.itau.portalmanager.workspace.input.web.catalog.workspace;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.workspace.WorkspaceType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.workspace.WorkspaceTypeService;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspace-type")
public class WorkspaceTypeController extends OwnerCatalogController<WorkspaceType> {

    public WorkspaceTypeController(WorkspaceTypeService service) {
        super(service);
    }
}
