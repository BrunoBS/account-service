package br.com.itau.portalmanager.workspace.input.web.catalog.environment;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.environment.EnvironmentType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.environment.EnvironmentTypeService;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/environment-type")
public class EnvironmentTypeController extends OwnerCatalogController<EnvironmentType> {

    public EnvironmentTypeController(EnvironmentTypeService service) {
        super(service);
    }
}
