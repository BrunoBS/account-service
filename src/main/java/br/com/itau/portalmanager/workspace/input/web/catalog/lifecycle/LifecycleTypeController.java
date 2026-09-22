package br.com.itau.portalmanager.workspace.input.web.catalog.lifecycle;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.lifecycle.LifecycleType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.lifecycle.LifecycleTypeService;
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
