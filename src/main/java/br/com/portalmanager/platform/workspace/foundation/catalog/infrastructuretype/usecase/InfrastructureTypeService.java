package br.com.portalmanager.platform.workspace.foundation.catalog.infrastructuretype.usecase;

import br.com.portalmanager.platform.workspace.foundation.catalog.infrastructuretype.domain.InfrastructureType;
import br.com.portalmanager.platform.workspace.foundation.catalog.infrastructuretype.domain.InfrastructureTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.infrastructuretype.repository.InfrastructureTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
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
