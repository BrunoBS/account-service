package br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSettingsValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleType;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.repository.LifecycleTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class LifecycleTypeService extends EnumCatalogService<LifecycleType, LifecycleTypeEnum> {

    private static final String SCHEMA_TYPE_CODE = "LIFECYCLE_TYPE";

    public LifecycleTypeService(
            LifecycleTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                LifecycleType.class,
                LifecycleTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(SCHEMA_TYPE_CODE, dto.settings(), result)
        );
    }
}
