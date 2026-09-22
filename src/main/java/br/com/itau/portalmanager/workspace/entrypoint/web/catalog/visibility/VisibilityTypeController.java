package br.com.itau.portalmanager.workspace.entrypoint.web.catalog.visibility;

import br.com.itau.portalmanager.workspace.foundation.catalog.visibilitytype.domain.VisibilityType;
import br.com.itau.portalmanager.workspace.foundation.catalog.visibilitytype.usecase.VisibilityTypeService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
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
