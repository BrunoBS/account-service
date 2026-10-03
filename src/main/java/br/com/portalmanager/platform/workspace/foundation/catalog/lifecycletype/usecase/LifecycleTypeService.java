package br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleType;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.repository.LifecycleTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class LifecycleTypeService extends EnumCatalogService<LifecycleType, LifecycleTypeEnum> {

    private static final String SCHEMA_RESOURCE_CODE = "lifecycle-type";

    public LifecycleTypeService(
            LifecycleTypeRepository repository,
            ObjectMapper objectMapper,
            SchemaValidator schemaValidator) {
        super(
                repository,
                objectMapper,
                LifecycleType.class,
                LifecycleTypeEnum.class,
                SCHEMA_RESOURCE_CODE,
                schemaValidator
        );
    }
}
