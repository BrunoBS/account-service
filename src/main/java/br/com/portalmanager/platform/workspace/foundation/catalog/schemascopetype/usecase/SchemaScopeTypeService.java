package br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.workspace.foundation.catalog.integration.CatalogSettingsValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.repository.SchemaScopeTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SchemaScopeTypeService extends EnumCatalogService<SchemaScopeType, SchemaScopeTypeEnum> {

    private static final String SCHEMA_TYPE_CODE = "SCHEMA_SCOPE_TYPE";

    public SchemaScopeTypeService(
            SchemaScopeTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSettingsValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                SchemaScopeType.class,
                SchemaScopeTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(SCHEMA_TYPE_CODE, dto.settings(), result)
        );
    }
}
