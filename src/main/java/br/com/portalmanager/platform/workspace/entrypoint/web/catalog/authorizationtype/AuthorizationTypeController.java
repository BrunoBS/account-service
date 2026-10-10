package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.authorizationtype;

import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationType;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.facade.AuthorizationTypeFacade;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/authorization-type")
public class AuthorizationTypeController extends CatalogController<AuthorizationType> {

    public AuthorizationTypeController(AuthorizationTypeFacade facade) {
        super(facade);
    }
}
