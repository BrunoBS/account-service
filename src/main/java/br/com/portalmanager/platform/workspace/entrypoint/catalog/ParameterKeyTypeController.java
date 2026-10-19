package br.com.portalmanager.platform.workspace.entrypoint.catalog;

import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.domain.ParameterKeyType;
import br.com.portalmanager.platform.workspace.foundation.catalog.parameterkeytype.facade.ParameterKeyTypeFacade;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parameter-key-type")
public class ParameterKeyTypeController extends CatalogController<ParameterKeyType> {

    public ParameterKeyTypeController(ParameterKeyTypeFacade facade) {
        super(facade);
    }
}
