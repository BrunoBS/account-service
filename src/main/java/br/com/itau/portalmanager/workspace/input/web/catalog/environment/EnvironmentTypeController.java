package br.com.itau.portalmanager.workspace.input.web.catalog.environment;

import br.com.itau.portalmanager.workspace.foundation.catalog.environment.domain.EnvironmentType;
import br.com.itau.portalmanager.workspace.foundation.catalog.environment.usecase.EnvironmentTypeService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
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
