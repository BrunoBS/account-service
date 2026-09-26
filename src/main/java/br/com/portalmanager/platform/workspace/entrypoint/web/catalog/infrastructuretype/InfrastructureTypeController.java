package br.com.portalmanager.platform.workspace.entrypoint.web.catalog.infrastructuretype;

import br.com.portalmanager.platform.workspace.foundation.catalog.infrastructuretype.domain.InfrastructureType;
import br.com.portalmanager.platform.workspace.foundation.catalog.infrastructuretype.usecase.InfrastructureTypeService;
import br.com.portalmanager.platform.library.catalog.web.CatalogController;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
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
