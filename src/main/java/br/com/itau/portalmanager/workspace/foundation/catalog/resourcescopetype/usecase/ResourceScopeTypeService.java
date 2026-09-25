package br.com.itau.portalmanager.workspace.foundation.catalog.resourcescopetype.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.resourcescopetype.domain.ResourceScopeTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.resourcescopetype.repository.ResourceScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class ResourceScopeTypeService extends EnumCatalogService<ResourceScopeType, ResourceScopeTypeEnum> {

    public ResourceScopeTypeService(
            ResourceScopeTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                ResourceScopeType.class,
                ResourceScopeTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
