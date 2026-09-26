package br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.usecase;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusType;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.repository.SchemaVersionStatusTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SchemaVersionStatusTypeService
        extends EnumCatalogService<SchemaVersionStatusType, SchemaVersionStatusTypeEnum> {

    public SchemaVersionStatusTypeService(
            SchemaVersionStatusTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator
    ) {
        super(
                repository,
                objectMapper,
                SchemaVersionStatusType.class,
                SchemaVersionStatusTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
