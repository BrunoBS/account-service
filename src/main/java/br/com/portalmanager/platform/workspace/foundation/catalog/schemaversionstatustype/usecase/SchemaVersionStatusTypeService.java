package br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.usecase;

import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusType;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.domain.SchemaVersionStatusTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemaversionstatustype.repository.SchemaVersionStatusTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSettingsValidator;
import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SchemaVersionStatusTypeService
        extends EnumCatalogService<SchemaVersionStatusType, SchemaVersionStatusTypeEnum> {

    public SchemaVersionStatusTypeService(
            SchemaVersionStatusTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsValidator settingsValidator
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
