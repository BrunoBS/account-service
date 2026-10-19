package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository.HierarchyReference;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional
public class EnvironmentFinder {

    private final EnvironmentRepository repository;
    private final EntityManager entityManager;

    public EnvironmentFinder(EnvironmentRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    public Environment findActive(String identifier, Long workspaceId) {
        Environment environment = find(identifier, workspaceId);
        if (!isAccessible(environment)) throw notFound();
        return environment;
    }

    public Environment findActiveForUpdate(String identifier, Long workspaceId) {
        Environment environment = findForUpdate(identifier, workspaceId, true);
        if (!LifecycleTypeCode.active().equals(environment.getLifecycle())) throw notFound();
        return environment;
    }

    @ResourceVisibility(Environment.class)
    public Environment findWorkspaceActiveForUpdate(String identifier, Long workspaceId) {
        return findActiveForUpdate(identifier, workspaceId);
    }

    public boolean isAccessible(Environment environment) {
        for (Environment current = environment; current != null;) {
            if (!LifecycleTypeCode.active().equals(current.getLifecycle())) return false;
            if (current.getParent() == null) return true;
            Optional<Environment> parent = repository.findVisibleById(current.getParent().getId());
            if (parent.isEmpty()) return false;
            current = parent.get();
        }
        return true;
    }

    public Environment findInactive(String identifier, Long workspaceId) {
        Environment environment = find(identifier, workspaceId);
        environment.requireRestorable();
        if (!hasVisibleAncestors(environment)) throw notFound();
        return environment;
    }

    public Environment findInactiveForRestore(String identifier, Long workspaceId) {
        Environment environment = findForUpdate(identifier, workspaceId, true);
        environment.requireRestorable();
        return environment;
    }

    @ResourceVisibility(Environment.class)
    public Environment findWorkspaceInactiveForRestore(String identifier, Long workspaceId) {
        return findInactiveForRestore(identifier, workspaceId);
    }

    public Environment findInactiveForDeletion(String identifier, Long workspaceId) {
        Environment environment = find(identifier, workspaceId);
        environment.requireDeletable();
        if (!hasVisibleAncestors(environment)) throw notFound();
        return environment;
    }

    public Environment findInactiveForDeletionForUpdate(String identifier, Long workspaceId) {
        Environment environment = findForUpdate(identifier, workspaceId, true);
        environment.requireDeletable();
        return environment;
    }

    @ResourceVisibility(Environment.class)
    public Environment findWorkspaceInactiveForDeletionForUpdate(String identifier, Long workspaceId) {
        return findInactiveForDeletionForUpdate(identifier, workspaceId);
    }

    public String findParentTypeCodeForValidation(Environment environment) {
        if (environment.getParent() == null) return null;
        return repository.findHierarchyReferenceForRead(environment.getParent().getId())
            .orElseThrow(this::notFound).getTypeCode();
    }

    private boolean hasVisibleAncestors(Environment environment) {
        Environment current = environment;
        while (current.getParent() != null) {
            Optional<Environment> parent = repository.findVisibleById(current.getParent().getId());
            if (parent.isEmpty()) return false;
            current = parent.get();
        }
        return true;
    }

    private Environment find(String identifier, Long workspaceId) {
        Optional<Environment> environment = workspaceId == null
            ? repository.findByIdentifierAndWorkspaceIdIsNull(identifier)
            : repository.findByIdentifierAndWorkspaceId(identifier, workspaceId);
        return environment.orElseThrow(this::notFound);
    }

    private Environment findForUpdate(String identifier, Long workspaceId, boolean requireActiveAncestors) {
        Environment snapshot = find(identifier, workspaceId);
        if (requireActiveAncestors) lockActiveAncestors(snapshot);
        // Discard managed snapshot state before acquiring the authoritative write lock.
        entityManager.detach(snapshot);
        return repository.findByIdentifierForUpdate(identifier)
            .filter(environment -> workspaceId == null
                ? environment.getWorkspaceId() == null
                : workspaceId.equals(environment.getWorkspaceId()))
            .orElseThrow(this::notFound);
    }

    private void lockActiveAncestors(Environment environment) {
        List<Long> ancestorIds = new ArrayList<>();
        Long ancestorId = environment.getParent() == null ? null : environment.getParent().getId();
        while (ancestorId != null) {
            HierarchyReference ancestor = repository.findHierarchyReference(ancestorId).orElseThrow(this::notFound);
            ancestorIds.add(ancestorId);
            ancestorId = ancestor.getParentId();
        }
        Collections.reverse(ancestorIds);
        for (Long lockedAncestorId : ancestorIds) {
            // JPQL preserves the visibility filter; the locking query checks the current lifecycle.
            repository.findVisibleByIdAndLifecycleForRead(lockedAncestorId, LifecycleTypeCode.active())
                .orElseThrow(this::notFound);
        }
    }

    private NotFoundException notFound() {
        return new NotFoundException(EnvironmentMessageKeys.NOT_FOUND);
    }
}
