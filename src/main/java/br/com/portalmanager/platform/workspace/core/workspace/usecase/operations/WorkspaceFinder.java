package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceMessageKeys;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.repository.projection.WorkspaceReference;
import br.com.portalmanager.platform.workspace.core.workspace.repository.projection.WorkspaceOnboardingReference;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

@Component
public class WorkspaceFinder {

    private final WorkspaceRepository repository;
    private final EntityManager entityManager;

    public WorkspaceFinder(WorkspaceRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @ResourceVisibility(Workspace.class)
    public Workspace findActiveForUpdate(String identifier) {
        Workspace workspace = repository
            .findByIdentifierForUpdate(identifier)
            .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
        entityManager.detach(workspace);
        workspace = repository
            .findByIdentifierForUpdate(identifier)
            .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
        WorkspaceValidator.requireActive(workspace);
        return workspace;
    }

    @ResourceVisibility(Workspace.class)
    public WorkspaceReference findActiveReference(String identifier) {
        return repository
            .findActiveReferenceByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
    }

    @ResourceVisibility(Workspace.class)
    public WorkspaceOnboardingReference findActiveOnboardingReference(String identifier) {
        return repository
            .findActiveOnboardingReferenceByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
    }

    @ResourceVisibility(Workspace.class)
    public WorkspaceReference findActiveReference(Long id) {
        return repository
            .findReferenceById(id, LifecycleTypeCode.active().value())
            .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
    }

    @ResourceVisibility(Workspace.class)
    public Workspace findActive(String identifier) {
        Workspace workspace = findRequired(identifier);
        WorkspaceValidator.requireActive(workspace);
        return workspace;
    }

    @ResourceVisibility(Workspace.class)
    public Workspace findInactiveForRestore(String identifier) {
        Workspace workspace = findRequired(identifier);
        workspace.requireRestorable();
        return workspace;
    }

    @ResourceVisibility(Workspace.class)
    public Workspace findInactiveForDeletion(String identifier) {
        Workspace workspace = findRequired(identifier);
        workspace.requireDeletable();
        return workspace;
    }

    private Workspace findRequired(String identifier) {
        return repository
            .findByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND));
    }
}
