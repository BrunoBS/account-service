package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.visibilitytype;

import br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.domain.VisibilityType;
import br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.usecase.VisibilityTypeService;
import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visibility-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class VisibilityTypeController extends CatalogController<VisibilityType> {

    public VisibilityTypeController(VisibilityTypeService service) {
        super(service);
    }
}
