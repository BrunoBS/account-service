package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.visibility;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.visibility.VisibilityType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.visibility.VisibilityTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.visibility.VisibilityTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.SchemaValidator;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class VisibilityTypeService extends EnumCatalogService<VisibilityType, VisibilityTypeEnum> {

    public VisibilityTypeService(
            VisibilityTypeRepository repository,
            ObjectMapper objectMapper,
            SchemaValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                VisibilityType.class,
                VisibilityTypeEnum.class,
                (dto, result) -> settingsValidator.validateJson(SchemaDefaults.DEFAULT_JSON_SCHEMA, dto.settings(), "settings", result)
        );
    }
}
