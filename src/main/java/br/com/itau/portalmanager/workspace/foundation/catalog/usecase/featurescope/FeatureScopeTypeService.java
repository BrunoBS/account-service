package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.featurescope;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.featurescope.FeatureScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.featurescope.FeatureScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.support.CatalogSettingsSchemaValidator;
import br.com.portalmanager.platform.catalog.service.DynamicCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class FeatureScopeTypeService extends DynamicCatalogService<FeatureScopeType> {

    public FeatureScopeTypeService(
            FeatureScopeTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsSchemaValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                FeatureScopeType.class,
                (dto, result) -> settingsValidator.validate(dto.settings(), "settings", result)
        );
    }
}
