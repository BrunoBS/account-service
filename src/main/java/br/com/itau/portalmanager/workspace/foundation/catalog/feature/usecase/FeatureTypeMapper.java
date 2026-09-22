package br.com.itau.portalmanager.workspace.foundation.catalog.feature.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.feature.domain.FeatureType;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.mapper.BaseCatalogMapper;
import org.springframework.stereotype.Component;

@Component
public class FeatureTypeMapper extends BaseCatalogMapper<FeatureTypeDTO, FeatureType> {

    private final CatalogSchemaValidationSupport settingsValidator;

    public FeatureTypeMapper(CatalogSchemaValidationSupport settingsValidator) {
        super(FeatureType.class);
        this.settingsValidator = settingsValidator;
    }

    @Override
    public FeatureTypeDTO toDTO(FeatureType entity) {
        if (entity == null) return null;
        return new FeatureTypeDTO(
                entity.getId(), entity.getName(), entity.getLabel(), entity.getDescription(),
                entity.getSortOrder(), settingsValidator.fromString(entity.getSettings()),
                entity.getFeatureScope() == null ? null : entity.getFeatureScope().getId(),
                entity.getFeatureScope() == null ? null : entity.getFeatureScope().getName(),
                entity.isAvailable()
        );
    }
}
