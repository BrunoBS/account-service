package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.workspace.core.environment.domain.EnvironmentMessageKeys;
import br.com.portalmanager.platform.workspace.core.environment.domain.environment.Environment;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository.HierarchyReference;
import br.com.portalmanager.platform.workspace.core.environment.repository.environment.EnvironmentRepository.IdentifierReference;
import br.com.portalmanager.platform.workspace.core.environment.usecase.mapping.environment.EnvironmentMapper;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentReferenceOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentTreeOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.port.WorkspaceReferencePort;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleFilter;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class EnvironmentQueryService {

    private final EnvironmentRepository repository;
    private final EnvironmentFinder finder;
    private final WorkspaceReferencePort workspaces;
    private final EnvironmentMapper mapper;

    public EnvironmentQueryService(
            EnvironmentRepository repository,
            EnvironmentFinder finder,
            WorkspaceReferencePort workspaces,
            EnvironmentMapper mapper) {
        this.repository = repository;
        this.finder = finder;
        this.workspaces = workspaces;
        this.mapper = mapper;
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public EnvironmentOutput findWorkspace(String workspaceIdentifier, String identifier) {
        return mapper.toOutput(
                finder.findActive(identifier, workspaces.resolveInternalId(workspaceIdentifier)),
                workspaceIdentifier);
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public EnvironmentOutput findWorkspaceInactive(String workspaceIdentifier, String identifier) {
        return mapper.toOutput(
                finder.findInactive(identifier, workspaces.resolveInternalId(workspaceIdentifier)),
                workspaceIdentifier);
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public EnvironmentOutput findWorkspaceForDeletion(
            String workspaceIdentifier, String identifier) {
        return mapper.toOutput(
                finder.findInactiveForDeletion(
                        identifier, workspaces.resolveInternalId(workspaceIdentifier)),
                workspaceIdentifier);
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public List<EnvironmentOutput> listWorkspace(String workspaceIdentifier, String lifecycle) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        LifecycleTypeCode lifecycleCode = LifecycleFilter.resolve(lifecycle);
        Map<Long, HierarchyReference> hierarchy =
                repository.findVisibleHierarchyInWorkspace(workspaceId).stream()
                        .collect(
                                Collectors.toMap(
                                        HierarchyReference::getId, reference -> reference));
        List<Environment> environments =
                repository.findByWorkspaceAndLifecycle(workspaceId, lifecycleCode).stream()
                        .filter(
                                environment ->
                                        hasVisibleAncestors(
                                                environment.getId(),
                                                hierarchy,
                                                LifecycleTypeCode.active().equals(lifecycleCode)))
                        .toList();
        return toOutputs(environments, workspaceIdentifier);
    }

    /**
     * Resolves environment references in bulk, retaining inactive environments in Shared history.
     */
    @Transactional(readOnly = true)
    public Map<Long, String> findIdentifiersByInternalIds(Collection<Long> environmentIds) {
        if (environmentIds.isEmpty()) return Map.of();
        return repository.findIdentifierReferences(environmentIds).stream()
                .collect(
                        Collectors.toMap(
                                IdentifierReference::getId, IdentifierReference::getIdentifier));
    }

    /**
     * Cross-application Shared read. Callers must first authorize the receiving application and
     * verify an active contract/participation before exposing this result.
     */
    @Transactional(readOnly = true)
    public List<EnvironmentOutput> listActiveForShared(String workspaceIdentifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        List<Environment> environments =
                repository.findVisibleInWorkspace(workspaceId, LifecycleTypeCode.active());
        Map<Long, Environment> hierarchy =
                environments.stream()
                        .collect(Collectors.toMap(Environment::getId, environment -> environment));
        return environments.stream()
                .sorted(Comparator.comparing(environment -> environment.getWorkspaceId() != null))
                .filter(environment -> hasActiveAncestors(environment, hierarchy))
                .map(
                        environment ->
                                mapper.toOutput(
                                        environment,
                                        environment.getWorkspaceId() == null
                                                ? null
                                                : workspaceIdentifier))
                .toList();
    }

    /**
     * Resolves only the requested environments, retaining workspace and ancestor activity checks.
     */
    @Transactional(readOnly = true)
    public Map<String, EnvironmentReferenceOutput> findAvailableReferencesForResourceSharing(
            String workspaceIdentifier, Collection<String> identifiers) {
        if (identifiers.isEmpty()) return Map.of();
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Map<Long, HierarchyReference> hierarchy =
                repository.findHierarchyForSharedReferences(workspaceId, identifiers).stream()
                        .collect(
                                Collectors.toMap(
                                        HierarchyReference::getId, reference -> reference));
        Map<String, EnvironmentReferenceOutput> available =
                repository.findSharedReferences(workspaceId, identifiers).stream()
                        .filter(
                                environment ->
                                        hasVisibleAncestors(environment.getId(), hierarchy, true))
                        .collect(
                                Collectors.toMap(
                                        Environment::getIdentifier,
                                        environment ->
                                                mapper.toReference(
                                                        environment.getId(),
                                                        mapper.toOutput(
                                                                environment,
                                                                environment.getWorkspaceId() == null
                                                                        ? null
                                                                        : workspaceIdentifier))));
        if (!available.keySet().containsAll(identifiers)) {
            throw new NotFoundException(EnvironmentMessageKeys.NOT_FOUND);
        }
        return available;
    }

    /** Resolves a Shared environment in the workspace or in the global defaults. */
    @Transactional(readOnly = true)
    public EnvironmentOutput findAvailableForShared(
            String workspaceIdentifier, String environmentIdentifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Environment environment =
                repository
                        .findByIdentifierAndWorkspaceId(environmentIdentifier, workspaceId)
                        .or(
                                () ->
                                        repository.findByIdentifierAndWorkspaceIdIsNull(
                                                environmentIdentifier))
                        .filter(finder::isAccessible)
                        .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.NOT_FOUND));
        return mapper.toOutput(
                environment, environment.getWorkspaceId() == null ? null : workspaceIdentifier);
    }

    @Transactional(readOnly = true)
    public EnvironmentOutput findDefault(String identifier) {
        return mapper.toOutput(finder.findActive(identifier, null), null);
    }

    @Transactional(readOnly = true)
    public EnvironmentOutput findDefaultInactive(String identifier) {
        return mapper.toOutput(finder.findInactive(identifier, null), null);
    }

    @Transactional(readOnly = true)
    public EnvironmentOutput findDefaultForDeletion(String identifier) {
        return mapper.toOutput(finder.findInactiveForDeletion(identifier, null), null);
    }

    @Transactional(readOnly = true)
    public List<EnvironmentOutput> listDefaults(String lifecycle) {
        LifecycleTypeCode lifecycleCode = LifecycleFilter.resolve(lifecycle);
        List<Environment> environments = repository.findDefaultsByLifecycle(lifecycleCode);
        Map<Long, Environment> hierarchy =
                environments.stream()
                        .collect(Collectors.toMap(Environment::getId, environment -> environment));
        List<Environment> accessibleEnvironments =
                environments.stream()
                        .filter(
                                environment ->
                                        !LifecycleTypeCode.active().equals(lifecycleCode)
                                                || hasActiveAncestors(environment, hierarchy))
                        .toList();
        return toOutputs(accessibleEnvironments, null);
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public List<EnvironmentOutput> findRoots(String workspaceIdentifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        List<Environment> defaultRoots = repository.findDefaultRoots(LifecycleTypeCode.active());
        return Stream.concat(
                        defaultRoots.stream(),
                        repository
                                .findWorkspaceRoots(workspaceId, LifecycleTypeCode.active())
                                .stream())
                .map(
                        environment ->
                                mapper.toOutput(
                                        environment,
                                        environment.getWorkspaceId() == null
                                                ? null
                                                : workspaceIdentifier))
                .toList();
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public List<EnvironmentOutput> findChildren(
            String workspaceIdentifier, String parentIdentifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Environment parent =
                repository
                        .findByIdentifier(parentIdentifier)
                        .orElseThrow(() -> new NotFoundException(EnvironmentMessageKeys.NOT_FOUND));
        if ((parent.getWorkspaceId() != null && !parent.getWorkspaceId().equals(workspaceId))
                || !finder.isAccessible(parent))
            throw new NotFoundException(EnvironmentMessageKeys.NOT_FOUND);
        return repository
                .findChildren(parent.getId(), workspaceId, LifecycleTypeCode.active())
                .stream()
                .map(environment -> mapper.toOutput(environment, workspaceIdentifier))
                .toList();
    }

    @ResourceVisibility(Environment.class)
    @Transactional(readOnly = true)
    public List<EnvironmentTreeOutput> findTree(String workspaceIdentifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        List<Environment> visibleEnvironments =
                repository.findVisibleInWorkspace(workspaceId, LifecycleTypeCode.active());
        Map<Long, List<Environment>> childrenByParentId =
                visibleEnvironments.stream()
                        .filter(environment -> environment.getParent() != null)
                        .collect(
                                Collectors.groupingBy(
                                        environment -> environment.getParent().getId()));
        return visibleEnvironments.stream()
                .filter(environment -> environment.getParent() == null)
                .map(environment -> buildTree(environment, childrenByParentId, workspaceIdentifier))
                .toList();
    }

    private List<EnvironmentOutput> toOutputs(
            List<Environment> environments, String workspaceIdentifier) {
        Set<Long> parentIds =
                environments.stream()
                        .filter(environment -> environment.getParent() != null)
                        .map(environment -> environment.getParent().getId())
                        .collect(Collectors.toSet());
        Map<Long, String> parentIdentifiers = findIdentifiersByInternalIds(parentIds);
        return environments.stream()
                .map(
                        environment -> {
                            String parentIdentifier = null;
                            if (environment.getParent() != null) {
                                parentIdentifier =
                                        parentIdentifiers.get(environment.getParent().getId());
                                if (parentIdentifier == null)
                                    throw new NotFoundException(EnvironmentMessageKeys.NOT_FOUND);
                            }
                            return mapper.toOutput(
                                    environment, workspaceIdentifier, parentIdentifier);
                        })
                .toList();
    }

    private boolean hasActiveAncestors(Environment environment, Map<Long, Environment> hierarchy) {
        for (Environment current = environment; current != null; ) {
            if (!LifecycleTypeCode.active().equals(current.getLifecycle())) return false;
            if (current.getParent() == null) return true;
            current = hierarchy.get(current.getParent().getId());
            if (current == null) return false;
        }
        return true;
    }

    private boolean hasVisibleAncestors(
            Long environmentId, Map<Long, HierarchyReference> hierarchy, boolean requireActive) {
        for (Long currentId = environmentId; currentId != null; ) {
            HierarchyReference current = hierarchy.get(currentId);
            if (current == null
                    || requireActive
                            && !LifecycleTypeCode.active().value().equals(current.getLifecycle()))
                return false;
            currentId = current.getParentId();
        }
        return true;
    }

    private EnvironmentTreeOutput buildTree(
            Environment environment,
            Map<Long, List<Environment>> childrenByParentId,
            String workspaceIdentifier) {
        List<EnvironmentTreeOutput> children =
                childrenByParentId.getOrDefault(environment.getId(), List.of()).stream()
                        .map(child -> buildTree(child, childrenByParentId, workspaceIdentifier))
                        .toList();
        return mapper.toTreeOutput(
                mapper.toOutput(
                        environment,
                        environment.getWorkspaceId() == null ? null : workspaceIdentifier),
                children);
    }
}
