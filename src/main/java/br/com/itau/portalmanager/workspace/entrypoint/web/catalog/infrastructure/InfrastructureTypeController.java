package br.com.itau.portalmanager.workspace.entrypoint.web.catalog.infrastructure;

import br.com.itau.portalmanager.workspace.foundation.catalog.infrastructuretype.domain.InfrastructureType;
import br.com.itau.portalmanager.workspace.foundation.catalog.infrastructuretype.usecase.InfrastructureTypeService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/infrastructure-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class InfrastructureTypeController extends CatalogController<InfrastructureType> {

    public InfrastructureTypeController(InfrastructureTypeService service) {
        super(service);
    }
}
