package br.com.itau.portalmanager.workspace.foundation.catalog.usecase.publisherscope;

import br.com.itau.portalmanager.workspace.foundation.catalog.domain.publisherscope.PublisherScopeType;
import br.com.itau.portalmanager.workspace.foundation.catalog.domain.publisherscope.PublisherScopeTypeEnum;
import br.com.itau.portalmanager.workspace.foundation.catalog.repository.publisherscope.PublisherScopeTypeRepository;
import br.com.itau.portalmanager.workspace.foundation.catalog.usecase.support.CatalogSchemaValidationSupport;
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
