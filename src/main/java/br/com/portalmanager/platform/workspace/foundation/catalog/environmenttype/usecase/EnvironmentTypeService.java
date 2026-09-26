package br.com.portalmanager.platform.workspace.foundation.catalog.environmenttype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.workspace.foundation.catalog.environmenttype.domain.EnvironmentType;
import br.com.portalmanager.platform.workspace.foundation.catalog.environmenttype.domain.EnvironmentTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.environmenttype.repository.EnvironmentTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSettingsValidator;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class EnvironmentTypeService extends EnumCatalogService<EnvironmentType, EnvironmentTypeEnum> {

    public EnvironmentTypeService(
            EnvironmentTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                EnvironmentType.class,
                EnvironmentTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
