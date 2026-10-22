package br.com.portalmanager.platform.workspace.foundation.integration;

import br.com.portalmanager.platform.library.catalog.mapper.CatalogMapper;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.domain.PublisherType;
import br.com.portalmanager.platform.workspace.foundation.catalog.publishertype.repository.PublisherTypeRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.util.Collection;
import java.util.List;

@Component
public class PublisherReferenceAdapter implements PublisherReferencePort {
    private final PublisherTypeRepository repository;
    private final CatalogMapper<PublisherType> mapper;

    public PublisherReferenceAdapter(
            PublisherTypeRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.mapper = new CatalogMapper<>(PublisherType.class, objectMapper);
    }

    @Override
    @Transactional
    public List<PublisherReference> resolveForAssociation(Collection<String> codes) {
        return codes.isEmpty()
                ? List.of()
                : repository.findForAssociation(codes).stream()
                        .map(p -> new PublisherReference(p.getCode(), p.isActive(), settings(p)))
                        .toList();
    }

    private tools.jackson.databind.JsonNode settings(PublisherType publisher) {
        try {
            return mapper.toDTO(publisher).settings();
        } catch (
                br.com.portalmanager.platform.library.catalog.exception.CatalogTechnicalException
                        exception) {
            // An unreadable persisted settings document is exposed to Core as unconfigured.
            return null;
        }
    }
}
