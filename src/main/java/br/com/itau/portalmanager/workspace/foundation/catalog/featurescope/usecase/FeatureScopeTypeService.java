package br.com.itau.portalmanager.workspace.foundation.catalog.featurescope.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.featurescope.domain.FeatureScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.featurescope.repository.FeatureScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.DynamicCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class FeatureScopeTypeService extends DynamicCatalogService<FeatureScopeType> {

    public FeatureScopeTypeService(
            FeatureScopeTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                FeatureScopeType.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
