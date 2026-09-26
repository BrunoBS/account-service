package br.com.portalmanager.platform.workspace.foundation.catalog.schematype.usecase;

import br.com.portalmanager.platform.library.catalog.service.DynamicCatalogService;
import br.com.portalmanager.platform.workspace.foundation.catalog.schematype.domain.SchemaType;
import br.com.portalmanager.platform.workspace.foundation.catalog.schematype.repository.SchemaTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class SchemaTypeService extends DynamicCatalogService<SchemaType> {

    public SchemaTypeService(
            SchemaTypeRepository repository,
            ObjectMapper objectMapper
    ) {
        super(repository, objectMapper, SchemaType.class);
    }
}
