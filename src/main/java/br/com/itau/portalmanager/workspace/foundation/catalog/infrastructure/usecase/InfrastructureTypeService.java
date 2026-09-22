package br.com.itau.portalmanager.workspace.foundation.catalog.infrastructure.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.infrastructure.domain.InfrastructureType;
import br.com.itau.portalmanager.workspace.foundation.catalog.infrastructure.domain.InfrastructureTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.infrastructure.repository.InfrastructureTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class InfrastructureTypeService extends EnumCatalogService<InfrastructureType, InfrastructureTypeEnum> {

    public InfrastructureTypeService(
            InfrastructureTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                InfrastructureType.class,
                InfrastructureTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
