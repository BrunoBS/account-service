package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.schemascope;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.schemascope.SchemaScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.schemascope.SchemaScopeTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.schemascope.SchemaScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.SchemaValidator;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SchemaScopeTypeService extends EnumCatalogService<SchemaScopeType, SchemaScopeTypeEnum> {

    public SchemaScopeTypeService(
            SchemaScopeTypeRepository repository,
            ObjectMapper objectMapper,
            SchemaValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                SchemaScopeType.class,
                SchemaScopeTypeEnum.class,
                (dto, result) -> settingsValidator.validateJson(SchemaDefaults.DEFAULT_JSON_SCHEMA, dto.settings(), "settings", result)
        );
    }
}
