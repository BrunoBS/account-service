package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.infrastructure;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.infrastructure.InfrastructureType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.infrastructure.InfrastructureTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.infrastructure.InfrastructureTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.SchemaValidator;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class InfrastructureTypeService extends EnumCatalogService<InfrastructureType, InfrastructureTypeEnum> {

    public InfrastructureTypeService(
            InfrastructureTypeRepository repository,
            ObjectMapper objectMapper,
            SchemaValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                InfrastructureType.class,
                InfrastructureTypeEnum.class,
                (dto, result) -> settingsValidator.validateJson(SchemaDefaults.DEFAULT_JSON_SCHEMA, dto.settings(), "settings", result)
        );
    }
}
