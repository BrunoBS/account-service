package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceMessageKeys;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceTag;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.repository.projection.WorkspaceOnboardingReference;
import br.com.portalmanager.platform.workspace.core.workspace.repository.projection.WorkspaceReference;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.FindAllWorkspacesInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOnboardingReferenceOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.usecase.LifecycleFilter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceQueryService {

    private final WorkspaceRepository repository;
    private final WorkspaceFinder finder;
    private final WorkspaceValidator validator;
    private final TagManager<WorkspaceTag, Workspace> tags;

    public WorkspaceQueryService(
        WorkspaceRepository repository,
        WorkspaceFinder finder,
        WorkspaceValidator validator,
        TagManager<WorkspaceTag, Workspace> tags
    ) {
        this.repository = repository;
        this.finder = finder;
        this.validator = validator;
        this.tags = tags;
    }

    @Transactional(readOnly = true)
    @ResourceVisibility(Workspace.class)
    public String findReferenceIdentifierByInternalId(Long workspaceId) {
        return findReferenceIdentifier(workspaceId);
    }

    /** Only after Shared has authorized the contract or participation involving this workspace. */
    @Transactional(readOnly = true)
    public String findReferenceIdentifierForResourceSharing(Long workspaceId) {
        return findReferenceIdentifier(workspaceId);
    }

    private String findReferenceIdentifier(Long workspaceId) {
        return repository
            .findReferenceById(workspaceId, null)
            .orElseThrow(() -> new NotFoundException(WorkspaceMessageKeys.NOT_FOUND))
            .getIdentifier();
    }

    @Transactional(readOnly = true)
    @ResourceVisibility(Workspace.class)
    public Map<Long, String> findReferenceIdentifiersByInternalIds(Collection<Long> workspaceIds) {
        return findIdentifiersByInternalIds(workspaceIds, null);
    }

    @Transactional(readOnly = true)
    @ResourceVisibility(Workspace.class)
    public Map<Long, String> findActiveIdentifiersByInternalIds(Collection<Long> workspaceIds) {
        return findIdentifiersByInternalIds(workspaceIds, LifecycleTypeCode.active().value());
    }

    /** Historical cross-workspace references; the caller must authorize the Shared scope first. */
    @Transactional(readOnly = true)
    public Map<Long, String> findReferenceIdentifiersForResourceSharing(Collection<Long> workspaceIds) {
        return findIdentifiersByInternalIds(workspaceIds, null);
    }

    /** Cross-workspace discovery after authorization of the participant application. */
    @Transactional(readOnly = true)
    public Map<Long, String> findActiveIdentifiersForResourceSharing(Collection<Long> workspaceIds) {
        return findIdentifiersByInternalIds(workspaceIds, LifecycleTypeCode.active().value());
    }

    private Map<Long, String> findIdentifiersByInternalIds(Collection<Long> workspaceIds, String lifecycleCode) {
        if (workspaceIds.isEmpty()) return Map.of();
        return repository
            .findReferencesByIds(workspaceIds, lifecycleCode)
            .stream()
            .collect(Collectors.toMap(WorkspaceReference::getId, WorkspaceReference::getIdentifier));
    }

    @ResourceVisibility(Workspace.class)
    @Transactional(readOnly = true)
    public WorkspaceOutput findByIdentifier(String identifier) {
        Workspace workspace = finder.findActive(identifier);
        return WorkspaceOutput.from(workspace, tags.findManual(workspace));
    }

    @ResourceVisibility(Workspace.class)
    @Transactional(readOnly = true)
    public WorkspaceOnboardingReferenceOutput findOnboardingReference(String identifier) {
        WorkspaceOnboardingReference reference = finder.findActiveOnboardingReference(identifier);
        return new WorkspaceOnboardingReferenceOutput(
            reference.getId(),
            reference.getWorkspaceType(),
            reference.getOnboarding()
        );
    }

    @ResourceVisibility(Workspace.class)
    @Transactional(readOnly = true)
    public Page<WorkspaceOutput> findAll(FindAllWorkspacesInput input, Pageable pageable) {
        validator.validatePagination(pageable);
        String lifecycleCode = LifecycleFilter.resolve(input.lifecycle()).value();
        validator.validateTypeFilter(input.typeName());
        Sort workspaceSort =
            pageable.getSort().getOrderFor("id") == null ? pageable.getSort().and(Sort.by("id")) : pageable.getSort();

        Pageable orderedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), workspaceSort);
        Page<Workspace> workspaces = repository.findFiltered(
            lifecycleCode,
            input.typeName(),
            input.tagName() == null ? null : TagName.of(input.tagName()),
            orderedPageable
        );
        Map<String, List<String>> manualTagsByWorkspaceIdentifier = tags.findManualByOwnerKeys(
            workspaces.getContent().stream().map(Workspace::getIdentifier).toList()
        );
        return workspaces.map(workspace ->
            WorkspaceOutput.from(
                workspace,
                manualTagsByWorkspaceIdentifier.getOrDefault(workspace.getIdentifier(), List.of())
            )
        );
    }
}
