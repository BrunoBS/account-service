package br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.usecase.AuthorizationTypeService;
import br.com.portalmanager.platform.workspace.foundation.catalog.authorizationtype.domain.AuthorizationType;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationTypeFacade extends AbstractCatalogFacade<AuthorizationType> {
    public AuthorizationTypeFacade(AuthorizationTypeService service) {
        super(service);
    }
}
