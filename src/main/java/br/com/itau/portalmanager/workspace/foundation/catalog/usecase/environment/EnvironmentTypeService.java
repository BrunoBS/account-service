package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.environment;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.environment.EnvironmentType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.environment.EnvironmentTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.environment.EnvironmentTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.support.CatalogSettingsSchemaValidator;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class EnvironmentTypeService extends EnumCatalogService<EnvironmentType, EnvironmentTypeEnum> {

    public EnvironmentTypeService(
            EnvironmentTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsSchemaValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                EnvironmentType.class,
                EnvironmentTypeEnum.class,
                (dto, result) -> settingsValidator.validate(dto.settings(), "settings", result)
        );
    }
}
