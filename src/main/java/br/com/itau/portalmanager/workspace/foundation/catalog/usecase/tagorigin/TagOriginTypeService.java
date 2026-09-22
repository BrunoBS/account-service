package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.tagorigin;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.tagorigin.TagOriginType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.tagorigin.TagOriginTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.tagorigin.TagOriginTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.schema.domain.SchemaDefaults;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.SchemaValidator;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class TagOriginTypeService extends EnumCatalogService<TagOriginType, TagOriginTypeEnum> {

    public TagOriginTypeService(
            TagOriginTypeRepository repository,
            ObjectMapper objectMapper,
            SchemaValidator settingsValidator) {
        super(
                repository,
                objectMapper,
                TagOriginType.class,
                TagOriginTypeEnum.class,
                (dto, result) -> settingsValidator.validateJson(SchemaDefaults.DEFAULT_JSON_SCHEMA, dto.settings(), "settings", result)
        );
    }
}
