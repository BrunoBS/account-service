package br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.usecase;

import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.domain.PublisherType;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.domain.PublisherTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.repository.PublisherTypeRepository;

import org.springframework.stereotype.Service;

import tools.jackson.databind.ObjectMapper;

@Service
public class PublisherTypeService extends EnumCatalogService<PublisherType, PublisherTypeEnum> {

    private static final String SCHEMA_RESOURCE_CODE = "publisher-type";

    public PublisherTypeService(
            PublisherTypeRepository repository,
            ObjectMapper objectMapper,
            SchemaValidator schemaValidator) {
        super(
                repository,
                objectMapper,
                PublisherType.class,
                PublisherTypeEnum.class,
                SCHEMA_RESOURCE_CODE,
                schemaValidator);
    }
}
