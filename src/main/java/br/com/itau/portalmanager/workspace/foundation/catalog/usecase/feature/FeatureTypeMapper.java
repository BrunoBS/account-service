package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.feature;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.feature.FeatureType;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.SchemaValidator;
import br.com.portalmanager.platform.catalog.mapper.BaseCatalogMapper;
import org.springframework.stereotype.Component;

@Component
public class FeatureTypeMapper extends BaseCatalogMapper<FeatureTypeDTO, FeatureType> {

    private final SchemaValidator settingsValidator;

    public FeatureTypeMapper(SchemaValidator settingsValidator) {
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
