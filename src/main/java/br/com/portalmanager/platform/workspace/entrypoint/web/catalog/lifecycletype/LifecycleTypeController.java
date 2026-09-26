package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.lifecycletype;

import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleType;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleTypeService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
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
