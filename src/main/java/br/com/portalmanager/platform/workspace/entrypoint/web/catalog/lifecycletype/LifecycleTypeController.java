package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.lifecycletype;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleType;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleTypeService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/lifecycle-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class LifecycleTypeController extends CatalogController<LifecycleType> {

    public LifecycleTypeController(LifecycleTypeService service) {
        super(service);
    }
}
