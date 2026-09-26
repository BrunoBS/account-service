package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.environmenttype;

import br.com.portalmanager.platform.workspace.foundation.catalog.environmenttype.domain.EnvironmentType;
import br.com.portalmanager.platform.workspace.foundation.catalog.environmenttype.usecase.EnvironmentTypeService;
import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/environment-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class EnvironmentTypeController extends CatalogController<EnvironmentType> {

    public EnvironmentTypeController(EnvironmentTypeService service) {
        super(service);
    }
}
