package br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.usecase.VisibilityTypeService;
import br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.domain.VisibilityType;
import org.springframework.stereotype.Component;

@Component
public class VisibilityTypeFacade extends AbstractCatalogFacade<VisibilityType> {
    public VisibilityTypeFacade(VisibilityTypeService service) {
        super(service);
    }
}
