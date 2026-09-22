package br.com.itau.portalmanager.workspace.foundation.catalog.schematype.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.schematype.domain.SchemaType;
import br.com.itau.portalmanager.workspace.foundation.catalog.schematype.domain.SchemaTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.schematype.repository.SchemaTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SchemaTypeService extends EnumCatalogService<SchemaType, SchemaTypeEnum> {

    public SchemaTypeService(
            SchemaTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                SchemaType.class,
                SchemaTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
