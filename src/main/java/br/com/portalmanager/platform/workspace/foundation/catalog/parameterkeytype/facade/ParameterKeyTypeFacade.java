package br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.facade;

import br.com.portalmanager.platform.library.catalog.facade.AbstractCatalogFacade;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.domain.ParameterKeyType;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.usecase.ParameterKeyTypeService;

import org.springframework.stereotype.Component;

@Component
public class ParameterKeyTypeFacade extends AbstractCatalogFacade<ParameterKeyType> {

    public ParameterKeyTypeFacade(ParameterKeyTypeService service) {
        super(service);
    }
}
