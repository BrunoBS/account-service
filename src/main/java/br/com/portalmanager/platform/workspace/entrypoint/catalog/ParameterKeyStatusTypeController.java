package br.com.portalmanager.platform.workspace.entrypoint.catalog;

import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.domain.ParameterKeyStatusType;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeystatustype.facade.ParameterKeyStatusTypeFacade;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parameter-key-status-type")
public class ParameterKeyStatusTypeController extends CatalogController<ParameterKeyStatusType> {

    public ParameterKeyStatusTypeController(ParameterKeyStatusTypeFacade facade) {
        super(facade);
    }
}
