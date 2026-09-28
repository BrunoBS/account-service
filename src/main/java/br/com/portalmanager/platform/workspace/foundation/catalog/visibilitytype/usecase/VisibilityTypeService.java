package br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSettingsValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.domain.VisibilityType;
import br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.domain.VisibilityTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.visibilitytype.repository.VisibilityTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class VisibilityTypeService extends EnumCatalogService<VisibilityType, VisibilityTypeEnum> {

    private static final String SCHEMA_RESOURCE_CODE = "visibility-type";

    public VisibilityTypeService(
            VisibilityTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                VisibilityType.class,
                VisibilityTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(SCHEMA_RESOURCE_CODE, dto.settings(), result)
        );
    }
}
