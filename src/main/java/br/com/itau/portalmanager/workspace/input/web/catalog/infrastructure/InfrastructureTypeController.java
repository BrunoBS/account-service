package br.com.itau.portalmanager.workspace.input.web.catalog.infrastructure;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.infrastructure.InfrastructureType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.infrastructure.InfrastructureTypeService;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/infrastructure-type")
public class InfrastructureTypeController extends OwnerCatalogController<InfrastructureType> {

    public InfrastructureTypeController(InfrastructureTypeService service) {
        super(service);
    }
}
