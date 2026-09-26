package br.com.portalmanager.platform.workspace.foundation.catalog.integration;

import br.com.portalmanager.platform.workspace.foundation.catalog.schematype.usecase.SchemaTypeService;
import br.com.portalmanager.platform.workspace.foundation.integration.DynamicCatalogReferenceResolver;
import org.springframework.stereotype.Component;

@Component
public class FoundationDynamicCatalogReferenceResolver implements DynamicCatalogReferenceResolver {

    public static final String SCHEMA_TYPE = "schema-type";

    private final SchemaTypeService schemaTypeService;

    public FoundationDynamicCatalogReferenceResolver(SchemaTypeService schemaTypeService) {
        this.schemaTypeService = schemaTypeService;
    }

    @Override
    public boolean existsActive(String catalog, String code) {
        if (SCHEMA_TYPE.equals(catalog)) {
            return schemaTypeService.existsActive(code);
        }
        throw new IllegalArgumentException("Unsupported dynamic catalog: " + catalog);
    }
}
