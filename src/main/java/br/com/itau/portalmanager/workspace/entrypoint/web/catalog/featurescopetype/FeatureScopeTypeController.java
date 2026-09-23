package br.com.itau.portalmanager.workspace.entrypoint.web.catalog.featurescopetype;

import br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.domain.FeatureScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.featurescopetype.usecase.FeatureScopeTypeService;
import br.com.portalmanager.platform.catalog.web.CatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feature-scope")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class FeatureScopeTypeController extends CatalogController<FeatureScopeType> {

    public FeatureScopeTypeController(FeatureScopeTypeService service) {
        super(service);
    }
}
