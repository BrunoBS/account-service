package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.schematype;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.schematype.SchemaType;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.SchemaValidator;
import br.com.portalmanager.platform.catalog.mapper.BaseCatalogMapper;
import org.springframework.stereotype.Component;

@Component
public class SchemaTypeMapper extends BaseCatalogMapper<SchemaTypeDTO, SchemaType> {

    private final SchemaValidator settingsValidator;

    public SchemaTypeMapper(SchemaValidator settingsValidator) {
        super(SchemaType.class);
        this.settingsValidator = settingsValidator;
    }

    @Override
    public SchemaTypeDTO toDTO(SchemaType entity) {
        if (entity == null) return null;
        return new SchemaTypeDTO(
                entity.getId(), entity.getName(), entity.getLabel(), entity.getDescription(),
                entity.getSortOrder(),
                entity.getSchemaScope() == null ? null : entity.getSchemaScope().getName(),
                settingsValidator.fromString(entity.getSettings())
        );
    }
}
