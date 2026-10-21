package br.com.portalmanager.platform.workspace.core.publisher.integration;

import br.com.portalmanager.platform.workspace.core.publisher.domain.Publisher;
import br.com.portalmanager.platform.workspace.core.publisher.repository.PublisherRepository;
import br.com.portalmanager.platform.workspace.foundation.integration.PublisherReferencePort;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Component
public class PublisherReferenceAdapter implements PublisherReferencePort {
    private final PublisherRepository repository;

    public PublisherReferenceAdapter(PublisherRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public List<PublisherReference> resolveForAssociation(Collection<String> identifiers) {
        return identifiers.isEmpty()
                ? List.of()
                : repository.findForAssociation(identifiers).stream().map(this::reference).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublisherReference> findReferences(Collection<Long> ids) {
        return ids.isEmpty()
                ? List.of()
                : repository.findAllById(ids).stream().map(this::reference).toList();
    }

    private PublisherReference reference(Publisher publisher) {
        return new PublisherReference(
                publisher.getId(),
                publisher.getIdentifier(),
                publisher.getCode(),
                publisher.getScope().value(),
                publisher.getLifecycle().value(),
                publisher.isDeprecated());
    }
}
