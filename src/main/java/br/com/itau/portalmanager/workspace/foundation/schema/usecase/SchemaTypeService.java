package br.com.itau.portalmanager.workspace.foundation.schema.usecase;

import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaType;
import br.com.itau.portalmanager.workspace.foundation.schema.repository.SchemaTypeRepository;
import br.com.portalmanager.platform.catalog.service.DynamicCatalogService;
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
