package br.com.itau.portalmanager.workspace.input.web.catalog.featurescope;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.featurescope.FeatureScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.featurescope.FeatureScopeTypeService;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feature-scope")
public class FeatureScopeTypeController extends OwnerCatalogController<FeatureScopeType> {

    public FeatureScopeTypeController(FeatureScopeTypeService service) {
        super(service);
    }
}
