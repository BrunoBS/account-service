package br.com.itau.portalmanager.workspace.input.web.catalog.workspace;

import br.com.itau.portalmanager.workspace.foundation.catalog.workspacetype.domain.WorkspaceType;
import br.com.itau.portalmanager.workspace.foundation.catalog.workspacetype.usecase.WorkspaceTypeService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspace-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class WorkspaceTypeController extends CatalogController<WorkspaceType> {

    public WorkspaceTypeController(WorkspaceTypeService service) {
        super(service);
    }
}
