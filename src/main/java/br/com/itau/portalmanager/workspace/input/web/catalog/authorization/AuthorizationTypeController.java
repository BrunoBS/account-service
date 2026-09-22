package br.com.itau.portalmanager.workspace.input.web.catalog.authorization;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.authorization.AuthorizationType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.authorization.AuthorizationTypeService;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/authorization-type")
public class AuthorizationTypeController extends OwnerCatalogController<AuthorizationType> {

    public AuthorizationTypeController(AuthorizationTypeService service) {
        super(service);
    }
}
