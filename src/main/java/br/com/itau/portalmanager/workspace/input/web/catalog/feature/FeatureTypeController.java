package br.com.itau.portalmanager.workspace.input.web.catalog.feature;

import br.com.itau.portalmanager.workspace.foundation.catalog.feature.domain.FeatureType;
import br.com.itau.portalmanager.workspace.foundation.catalog.feature.usecase.FeatureTypeService;
import br.com.itau.portalmanager.workspace.foundation.catalog.feature.usecase.FeatureTypeDTO;
import br.com.portalmanager.platform.catalog.web.BaseCatalogController;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feature-type")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class FeatureTypeController extends BaseCatalogController<FeatureTypeDTO, FeatureType> {

    private final FeatureTypeService service;

    public FeatureTypeController(FeatureTypeService service) {
        this.service = service;
    }

    @Override
    protected FeatureTypeService getService() {
        return service;
    }
}
