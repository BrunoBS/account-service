package br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.domain.ParameterKeyStatusType;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.usecase.ParameterKeyStatusTypeService;

import org.springframework.stereotype.Component;

@Component
public class ParameterKeyStatusTypeFacade extends AbstractCatalogFacade<ParameterKeyStatusType> {

    public ParameterKeyStatusTypeFacade(ParameterKeyStatusTypeService service) {
        super(service);
    }
}
