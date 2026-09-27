package br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSettingsValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusType;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.repository.SchemaVersionStatusTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SchemaVersionStatusTypeService extends EnumCatalogService<SchemaVersionStatusType, SchemaVersionStatusTypeEnum> {

    private static final String SCHEMA_TYPE_CODE = "SCHEMA_VERSION_STATUS_TYPE";

    public SchemaVersionStatusTypeService(
            SchemaVersionStatusTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                SchemaVersionStatusType.class,
                SchemaVersionStatusTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(SCHEMA_TYPE_CODE, dto.settings(), result)
        );
    }
}
