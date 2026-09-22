package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.infrastructure;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.infrastructure.InfrastructureType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.infrastructure.InfrastructureTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.infrastructure.InfrastructureTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.support.CatalogSettingsSchemaValidator;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class InfrastructureTypeService extends EnumCatalogService<InfrastructureType, InfrastructureTypeEnum> {

    public InfrastructureTypeService(
            InfrastructureTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsSchemaValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                InfrastructureType.class,
                InfrastructureTypeEnum.class,
                (dto, result) -> settingsValidator.validate(dto.settings(), "settings", result)
        );
    }
}
