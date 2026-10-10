package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.lifecycletype;

import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleType;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.facade.LifecycleTypeFacade;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lifecycle-type")
public class LifecycleTypeController extends CatalogController<LifecycleType> {

    public LifecycleTypeController(LifecycleTypeFacade facade) {
        super(facade);
    }
}
