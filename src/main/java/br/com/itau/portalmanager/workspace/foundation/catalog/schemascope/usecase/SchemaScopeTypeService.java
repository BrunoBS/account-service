package br.com.itau.portalmanager.workspace.foundation.catalog.schemascope.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.schemascope.domain.SchemaScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.schemascope.domain.SchemaScopeTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.schemascope.repository.SchemaScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SchemaScopeTypeService extends EnumCatalogService<SchemaScopeType, SchemaScopeTypeEnum> {

    public SchemaScopeTypeService(
            SchemaScopeTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                SchemaScopeType.class,
                SchemaScopeTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
