package br.com.itau.portalmanager.workspace.foundation.catalog.environmenttype.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.environmenttype.domain.EnvironmentType;
import br.com.itau.portalmanager.workspace.foundation.catalog.environmenttype.domain.EnvironmentTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.environmenttype.repository.EnvironmentTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class EnvironmentTypeService extends EnumCatalogService<EnvironmentType, EnvironmentTypeEnum> {

    public EnvironmentTypeService(
            EnvironmentTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                EnvironmentType.class,
                EnvironmentTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
