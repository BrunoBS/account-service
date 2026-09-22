package br.com.itau.portalmanager.workspace.entrypoint.web.catalog.authorization;

import br.com.itau.portalmanager.workspace.foundation.catalog.authorizationtype.domain.AuthorizationType;
import br.com.itau.portalmanager.workspace.foundation.catalog.authorizationtype.usecase.AuthorizationTypeService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/authorization-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class AuthorizationTypeController extends CatalogController<AuthorizationType> {

    public AuthorizationTypeController(AuthorizationTypeService service) {
        super(service);
    }
}
