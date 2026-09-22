package br.com.itau.portalmanager.workspace.foundation.catalog.lifecycle.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycle.domain.LifecycleType;
import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycle.domain.LifecycleTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.lifecycle.repository.LifecycleTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class LifecycleTypeService extends EnumCatalogService<LifecycleType, LifecycleTypeEnum> {

    public LifecycleTypeService(
            LifecycleTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                LifecycleType.class,
                LifecycleTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
