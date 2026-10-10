package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.visibilitytype;

import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.domain.VisibilityType;
import br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.facade.VisibilityTypeFacade;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visibility-type")
public class VisibilityTypeController extends CatalogController<VisibilityType> {

    public VisibilityTypeController(VisibilityTypeFacade facade) {
        super(facade);
    }
}
