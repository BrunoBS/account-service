package br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleType;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleTypeService;
import org.springframework.stereotype.Component;

@Component
public class LifecycleTypeFacade extends AbstractCatalogFacade<LifecycleType> {

    public LifecycleTypeFacade(LifecycleTypeService service) {
        super(service);
    }
}
