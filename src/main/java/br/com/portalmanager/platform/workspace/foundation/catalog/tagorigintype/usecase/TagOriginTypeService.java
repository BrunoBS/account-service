package br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.domain.TagOriginType;
import br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.domain.TagOriginTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.repository.TagOriginTypeRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class TagOriginTypeService extends EnumCatalogService<TagOriginType, TagOriginTypeEnum> {

    private static final String SCHEMA_RESOURCE_CODE = "tag-origin-type";

    public TagOriginTypeService(
        TagOriginTypeRepository repository,
        ObjectMapper objectMapper,
        SchemaValidator schemaValidator
    ) {
        super(
            repository,
            objectMapper,
            TagOriginType.class,
            TagOriginTypeEnum.class,
            SCHEMA_RESOURCE_CODE,
            schemaValidator
        );
    }
}
