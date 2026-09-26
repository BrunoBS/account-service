package br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSettingsValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype.repository.ResourceScopeTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ResourceScopeTypeService extends EnumCatalogService<ResourceScopeType, ResourceScopeTypeEnum> {

    public ResourceScopeTypeService(
            ResourceScopeTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                ResourceScopeType.class,
                ResourceScopeTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
