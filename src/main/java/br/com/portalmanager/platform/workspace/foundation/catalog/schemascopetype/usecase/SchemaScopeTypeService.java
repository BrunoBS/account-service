package br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeType;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.domain.SchemaScopeTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.schemascopetype.repository.SchemaScopeTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SchemaScopeTypeService extends EnumCatalogService<SchemaScopeType, SchemaScopeTypeEnum> {

    private static final String SCHEMA_RESOURCE_CODE = "schema-scope-type";

    public SchemaScopeTypeService(
        SchemaScopeTypeRepository repository,
        ObjectMapper objectMapper,
        SchemaValidator schemaValidator
    ) {
        super(
            repository,
            objectMapper,
            SchemaScopeType.class,
            SchemaScopeTypeEnum.class,
            SCHEMA_RESOURCE_CODE,
            schemaValidator
        );
    }
}
