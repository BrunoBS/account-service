package br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.usecase.ApplicationScopeTypeService;
import org.springframework.stereotype.Component;

@Component
public class ApplicationScopeTypeFacade extends AbstractCatalogFacade<ApplicationScopeType> {

    public ApplicationScopeTypeFacade(ApplicationScopeTypeService service) {
        super(service);
    }
}
