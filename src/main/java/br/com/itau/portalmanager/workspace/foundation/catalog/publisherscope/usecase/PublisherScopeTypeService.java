package br.com.itau.portalmanager.workspace.foundation.catalog.publisherscope.usecase;

import br.com.itau.portalmanager.workspace.foundation.catalog.publisherscope.domain.PublisherScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.publisherscope.domain.PublisherScopeTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.publisherscope.repository.PublisherScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.support.CatalogSchemaValidationSupport;
import br.com.portalmanager.platform.catalog.service.EnumCatalogService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class PublisherScopeTypeService extends EnumCatalogService<PublisherScopeType, PublisherScopeTypeEnum> {

    public PublisherScopeTypeService(
            PublisherScopeTypeRepository repository,
            ObjectMapper objectMapper,
            CatalogSchemaValidationSupport settingsValidator) {
        super(
                repository,
                objectMapper,
                PublisherScopeType.class,
                PublisherScopeTypeEnum.class,
                (dto, result) -> settingsValidator.validateSettings(dto.settings(), result)
        );
    }
}
