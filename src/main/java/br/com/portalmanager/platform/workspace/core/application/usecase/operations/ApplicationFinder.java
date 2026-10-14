package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationMessageKeys;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class ApplicationFinder {

    private final ApplicationRepository repository;
    private final EntityManager entityManager;

    public ApplicationFinder(ApplicationRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @ResourceVisibility(Application.class)
    public Application findActive(String identifier, Long workspaceId) {
        Application application = repository
            .findByIdentifierAndWorkspaceId(identifier, workspaceId)
            .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND));
        if (!LifecycleTypeCode.active().equals(application.getLifecycle())) throw new NotFoundException(
            ApplicationMessageKeys.NOT_FOUND
        );
        return application;
    }

    @ResourceVisibility(Application.class)
    public Application findActiveForUpdate(String identifier, Long workspaceId) {
        Application application = repository
            .findByIdentifierAndWorkspaceIdForUpdate(identifier, workspaceId)
            .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND));
        // Reload with a locking query to avoid reusing an entity loaded before the lock was acquired.
        entityManager.detach(application);
        application = repository
            .findByIdentifierAndWorkspaceIdForUpdate(identifier, workspaceId)
            .filter(current -> LifecycleTypeCode.active().equals(current.getLifecycle()))
            .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND));
        return application;
    }

    @ResourceVisibility(Application.class)
    public Application findInactiveForRestore(String identifier, Long workspaceId) {
        Application application = repository
            .findByIdentifierAndWorkspaceId(identifier, workspaceId)
            .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND));
        application.requireRestorable();
        return application;
    }

    @ResourceVisibility(Application.class)
    public Application findInactiveForDeletion(String identifier, Long workspaceId) {
        Application application = repository
            .findByIdentifierAndWorkspaceId(identifier, workspaceId)
            .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND));
        application.requireDeletable();
        return application;
    }
}
