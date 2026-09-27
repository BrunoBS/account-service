package br.com.portalmanager.platform.workspace.core.publisher.usecase.operations;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.messaging.exception.ValidationException;
import br.com.portalmanager.platform.workspace.core.publisher.domain.Publisher;
import br.com.portalmanager.platform.workspace.core.publisher.domain.PublisherMessageKeys;
import br.com.portalmanager.platform.workspace.core.publisher.repository.PublisherRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.stereotype.Component;

@Component
public class PublisherFinder {
    private final PublisherRepository repository;
    public PublisherFinder(PublisherRepository repository) { this.repository = repository; }
    public Publisher findActive(String identifier) {
        Publisher publisher = find(identifier);
        if (!LifecycleTypeCode.active().equals(publisher.getLifecycle()))
            throw new NotFoundException(PublisherMessageKeys.NOT_FOUND);
        return publisher;
    }
    public Publisher findInactive(String identifier) {
        Publisher publisher = find(identifier);
        if (!LifecycleTypeCode.inactive().equals(publisher.getLifecycle()))
            throw new ValidationException(PublisherMessageKeys.RESTORE_INVALID);
        return publisher;
    }
    public Publisher findInactiveForDeletion(String identifier) {
        Publisher publisher = find(identifier);
        if (!LifecycleTypeCode.inactive().equals(publisher.getLifecycle()))
            throw new ValidationException(PublisherMessageKeys.DELETE_INVALID);
        return publisher;
    }
    private Publisher find(String identifier) {
        return repository.findByIdentifier(identifier)
                .orElseThrow(() -> new NotFoundException(PublisherMessageKeys.NOT_FOUND));
    }
}
