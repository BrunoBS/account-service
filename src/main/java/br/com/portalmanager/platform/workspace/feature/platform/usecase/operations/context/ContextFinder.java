package br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.context;

import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.feature.platform.domain.PlatformMessageKeys;
import br.com.portalmanager.platform.workspace.feature.platform.domain.context.Context;
import br.com.portalmanager.platform.workspace.feature.platform.repository.context.ContextRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class ContextFinder {
    private final ContextRepository repository;
    private final EntityManager entityManager;

    public ContextFinder(ContextRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public Context findActive(String contextIdentifier) {
        return repository.findByIdentifierAndLifecycle(contextIdentifier, LifecycleTypeCode.active())
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.CONTEXT_NOT_FOUND));
    }

    public Context findForUpdate(String contextIdentifier) {
        Long contextId = repository.findInternalIdByIdentifierForUpdate(contextIdentifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.CONTEXT_NOT_FOUND));
        // Reload after the scalar locking read, including entities already cached by JPA.
        entityManager.detach(entityManager.getReference(Context.class, contextId));
        return repository.findByIdentifierForUpdate(contextIdentifier)
            .orElseThrow(() -> new NotFoundException(PlatformMessageKeys.CONTEXT_NOT_FOUND));
    }
}
