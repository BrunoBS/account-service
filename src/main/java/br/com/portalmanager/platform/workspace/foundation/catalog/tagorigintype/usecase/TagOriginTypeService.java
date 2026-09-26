package br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.usecase;

import br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.domain.TagOriginType;
import br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.domain.TagOriginTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.tagorigintype.repository.TagOriginTypeRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class TagOriginTypeService extends EnumCatalogService<TagOriginType, TagOriginTypeEnum> {

    public TagOriginTypeService(
            TagOriginTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                TagOriginType.class,
                TagOriginTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
