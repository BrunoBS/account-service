package br.com.itau.portalmanager.workspace.input.web.catalog.feature;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.feature.FeatureType;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.feature.FeatureTypeService;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.feature.FeatureTypeDTO;
import br.com.itau.portalmanager.workspace.input.web.catalog.support.OwnerBaseCatalogController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feature-type")
public class FeatureTypeController extends OwnerBaseCatalogController<FeatureTypeDTO, FeatureType> {

    private final FeatureTypeService service;

    public FeatureTypeController(FeatureTypeService service) {
        this.service = service;
    }

    @Override
    protected FeatureTypeService getService() {
        return service;
    }
}
