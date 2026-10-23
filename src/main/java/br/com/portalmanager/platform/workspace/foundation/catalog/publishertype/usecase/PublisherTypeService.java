package br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.usecase;

import br.com.portalmanager.platform.library.catalog.exception.CatalogTechnicalException;
import br.com.portalmanager.platform.library.catalog.mapper.CatalogMapper;
import br.com.portalmanager.platform.library.catalog.service.EnumCatalogService;
import br.com.portalmanager.platform.library.schemavalidation.validation.SchemaValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.domain.PublisherType;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.domain.PublisherTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.repository.PublisherTypeRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.List;

@Service
public class PublisherTypeService extends EnumCatalogService<PublisherType, PublisherTypeEnum> {

    private final PublisherTypeRepository repository;
    private final CatalogMapper<PublisherType> mapper;
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
        this.repository = repository;
        this.mapper = new CatalogMapper<>(PublisherType.class, objectMapper);
    }

    /**
     * Published code/state/settings contract for association validation, including inactive
     * entries.
     */
    @Transactional
    public List<Reference> resolveForAssociation(Collection<String> codes) {
        if (codes.isEmpty()) return List.of();
        return repository.findForAssociation(codes).stream()
                .map(
                        publisher ->
                                new Reference(
                                        publisher.getCode(),
                                        publisher.isActive(),
                                        settings(publisher)))
                .toList();
    }

    private JsonNode settings(PublisherType publisher) {
        try {
            return mapper.toDTO(publisher).settings();
        } catch (CatalogTechnicalException exception) {
            return null;
        }
    }

    public record Reference(String code, boolean active, JsonNode settings) {}
}
