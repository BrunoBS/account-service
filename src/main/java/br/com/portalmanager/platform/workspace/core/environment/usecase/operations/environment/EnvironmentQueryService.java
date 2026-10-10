package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.repository.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentReferenceOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTreeOutput;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnvironmentQueryService {

    private final EnvironmentRepository repository;
    private final EnvironmentFinder finder;
    private final WorkspaceReferenceResolver workspaces;

    public EnvironmentQueryService(
        EnvironmentRepository repository,
        EnvironmentFinder finder,
        WorkspaceReferenceResolver workspaces
    ) {
        this.repository = repository;
        this.finder = finder;
        this.workspaces = workspaces;
    }

    @Transactional(readOnly = true)
    public Long findInternalIdByIdentifier(String identifier) {
        return repository
            .findByIdentifier(identifier)
            .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.NOT_FOUND))
            .getId();
    }

    @Transactional(readOnly = true)
    public String findIdentifierByInternalId(Long environmentId) {
        return repository
            .findById(environmentId)
            .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.NOT_FOUND))
            .getIdentifier();
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public EnvironmentOutput findCustom(String workspaceIdentifier, String identifier) {
        return EnvironmentOutput.from(
            finder.findActive(identifier, workspaces.resolveInternalId(workspaceIdentifier)),
            workspaceIdentifier
        );
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public EnvironmentOutput findCustomInactive(String workspaceIdentifier, String identifier) {
        return EnvironmentOutput.from(
            finder.findInactive(identifier, workspaces.resolveInternalId(workspaceIdentifier)),
            workspaceIdentifier
        );
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public EnvironmentOutput findCustomForDeletion(String workspaceIdentifier, String identifier) {
        return EnvironmentOutput.from(
            finder.findInactiveForDeletion(identifier, workspaces.resolveInternalId(workspaceIdentifier)),
            workspaceIdentifier
        );
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public List<EnvironmentOutput> listCustom(String workspaceIdentifier, Boolean active) {
        Long id = workspaces.resolveInternalId(workspaceIdentifier);
        return repository
            .findByWorkspaceAndLifecycle(id, lifecycle(active))
            .stream()
            .filter(e -> !Boolean.TRUE.equals(active) || finder.accessible(e))
            .map(e -> EnvironmentOutput.from(e, workspaceIdentifier))
            .toList();
    }

    /** Resolves environment references in bulk, retaining inactive environments in Shared history. */
    @Transactional(readOnly = true)
    public Map<Long, String> findIdentifiersByInternalIds(Collection<Long> environmentIds) {
        if (environmentIds.isEmpty()) return Map.of();
        return repository
            .findAllById(environmentIds)
            .stream()
            .collect(Collectors.toMap(Environment::getId, Environment::getIdentifier));
    }

    /**
     * Cross-application Shared read. Callers must first authorize the receiving application
     * and verify an active contract/participation before exposing this result.
     */
    @Transactional(readOnly = true)
    public List<EnvironmentOutput> listActiveForShared(String workspaceIdentifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        List<EnvironmentOutput> defaults = repository
            .findDefaultsByLifecycle(LifecycleTypeCode.active())
            .stream()
            .filter(finder::accessible)
            .map(e -> EnvironmentOutput.from(e, null))
            .toList();
        List<EnvironmentOutput> custom = repository
            .findByWorkspaceAndLifecycle(workspaceId, LifecycleTypeCode.active())
            .stream()
            .filter(finder::accessible)
            .map(e -> EnvironmentOutput.from(e, workspaceIdentifier))
            .toList();
        return java.util.stream.Stream.concat(defaults.stream(), custom.stream()).toList();
    }

    /** Cross-application Shared read after the owner contract and participant scope have been verified. */
    @Transactional(readOnly = true)
    public EnvironmentOutput findActiveForShared(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        return EnvironmentOutput.from(finder.findActive(identifier, workspaceId), workspaceIdentifier);
    }

    /** Resolves only the requested environments, retaining workspace and ancestor activity checks. */
    @Transactional(readOnly = true)
    public Map<String, EnvironmentReferenceOutput> findAvailableReferencesForShared(
        String workspaceIdentifier,
        Collection<String> identifiers
    ) {
        if (identifiers.isEmpty()) return Map.of();
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Map<String, EnvironmentReferenceOutput> available = repository
            .findSharedReferences(workspaceId, identifiers)
            .stream()
            .filter(finder::accessible)
            .collect(
                Collectors.toMap(Environment::getIdentifier, environment ->
                    new EnvironmentReferenceOutput(
                        environment.getId(),
                        EnvironmentOutput.from(
                            environment,
                            environment.getWorkspaceId() == null ? null : workspaceIdentifier
                        )
                    )
                )
            );
        if (!available.keySet().containsAll(identifiers)) {
            throw new NotFoundException(EnvironmentMessageKeys.NOT_FOUND);
        }
        return available;
    }

    /** Resolves a Shared environment in the workspace or in the global defaults. */
    @Transactional(readOnly = true)
    public EnvironmentOutput findAvailableForShared(String workspaceIdentifier, String environmentIdentifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Environment environment = repository
            .findByIdentifierAndWorkspaceId(environmentIdentifier, workspaceId)
            .or(() -> repository.findByIdentifierAndWorkspaceIdIsNull(environmentIdentifier))
            .filter(finder::accessible)
            .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.NOT_FOUND));
        return EnvironmentOutput.from(environment, environment.getWorkspaceId() == null ? null : workspaceIdentifier);
    }

    @Transactional(readOnly = true)
    public EnvironmentOutput findDefault(String identifier) {
        return EnvironmentOutput.from(finder.findActive(identifier, null), null);
    }

    @Transactional(readOnly = true)
    public EnvironmentOutput findDefaultInactive(String identifier) {
        return EnvironmentOutput.from(finder.findInactive(identifier, null), null);
    }

    @Transactional(readOnly = true)
    public EnvironmentOutput findDefaultForDeletion(String identifier) {
        return EnvironmentOutput.from(finder.findInactiveForDeletion(identifier, null), null);
    }

    @Transactional(readOnly = true)
    public List<EnvironmentOutput> listDefaults(Boolean active) {
        return repository
            .findDefaultsByLifecycle(lifecycle(active))
            .stream()
            .map(e -> EnvironmentOutput.from(e, null))
            .toList();
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public List<EnvironmentOutput> roots(String workspaceIdentifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        List<Environment> global = repository
            .findDefaultsByLifecycle(LifecycleTypeCode.active())
            .stream()
            .filter(e -> e.getParent() == null)
            .toList();
        return java.util.stream.Stream.concat(
            global.stream(),
            repository.findWorkspaceRoots(workspaceId, LifecycleTypeCode.active()).stream()
        )
            .map(e -> EnvironmentOutput.from(e, e.getWorkspaceId() == null ? null : workspaceIdentifier))
            .toList();
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public List<EnvironmentOutput> children(String workspaceIdentifier, String parentIdentifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Environment parent = repository
            .findByIdentifier(parentIdentifier)
            .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.NOT_FOUND));
        if (
            (parent.getWorkspaceId() != null && !parent.getWorkspaceId().equals(workspaceId)) ||
            !finder.accessible(parent)
        ) throw new NotFoundException(EnvironmentMessageKeys.NOT_FOUND);
        return repository
            .findChildren(parent.getId(), workspaceId, LifecycleTypeCode.active())
            .stream()
            .map(e -> EnvironmentOutput.from(e, workspaceIdentifier))
            .toList();
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public List<EnvironmentTreeOutput> tree(String workspaceIdentifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        List<Environment> globalRoots = repository
            .findDefaultsByLifecycle(LifecycleTypeCode.active())
            .stream()
            .filter(e -> e.getParent() == null)
            .toList();
        List<Environment> workspaceRoots = repository.findWorkspaceRoots(workspaceId, LifecycleTypeCode.active());
        return java.util.stream.Stream.concat(globalRoots.stream(), workspaceRoots.stream())
            .map(e -> buildTree(e, workspaceId, workspaceIdentifier))
            .toList();
    }

    private EnvironmentTreeOutput buildTree(Environment environment, Long workspaceId, String workspaceIdentifier) {
        List<EnvironmentTreeOutput> children = repository
            .findChildren(environment.getId(), workspaceId, LifecycleTypeCode.active())
            .stream()
            .map(e -> buildTree(e, workspaceId, workspaceIdentifier))
            .toList();
        return new EnvironmentTreeOutput(
            EnvironmentOutput.from(environment, environment.getWorkspaceId() == null ? null : workspaceIdentifier),
            children
        );
    }

    private LifecycleTypeCode lifecycle(Boolean active) {
        return Boolean.FALSE.equals(active) ? LifecycleTypeCode.inactive() : LifecycleTypeCode.active();
    }
}
