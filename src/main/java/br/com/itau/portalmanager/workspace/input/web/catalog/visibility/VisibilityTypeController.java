package br.com.itau.portalmanager.workspace.input.web.catalog.visibility;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.visibility.VisibilityType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.visibility.VisibilityTypeService;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/visibility-type")
public class VisibilityTypeController extends OwnerCatalogController<VisibilityType> {

    public VisibilityTypeController(VisibilityTypeService service) {
        super(service);
    }
}
