package br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.domain.FeatureType;
import br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.domain.FeatureTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.featuretype.repository.FeatureTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Service
public class FeatureTypeService extends EnumCatalogService<FeatureType, FeatureTypeEnum> {

    private final FeatureTypeRepository repository;

    public FeatureTypeService(
            FeatureTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                FeatureType.class,
                FeatureTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
        this.repository = repository;
    }

    public List<FeatureType> findActiveByNames(List<String> names) {
        return repository.findByCodeInAndActiveTrue(names);
    }
}
