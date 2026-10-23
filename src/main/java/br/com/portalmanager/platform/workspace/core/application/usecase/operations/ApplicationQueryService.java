package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.library.tagging.model.TagName;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationMessageKeys;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationTag;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import br.com.portalmanager.platform.workspace.core.application.repository.projection.ApplicationReference;
import br.com.portalmanager.platform.workspace.core.application.repository.projection.ApplicationScopeReference;
import br.com.portalmanager.platform.workspace.core.application.usecase.mapping.ApplicationMapper;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationSummary;
import br.com.portalmanager.platform.workspace.core.application.usecase.port.WorkspaceReferencePort;
import br.com.portalmanager.platform.workspace.core.application.usecase.validation.ApplicationValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleFilter;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ApplicationQueryService {

    private final ApplicationRepository repository;
    private final ApplicationFinder finder;
    private final WorkspaceReferencePort workspaces;
    private final TagManager<ApplicationTag, Application> tags;
    private final ApplicationNormalizer normalizer;
    private final ApplicationValidator validator;
    private final ApplicationMapper mapper;

    public ApplicationQueryService(
            ApplicationRepository repository,
            ApplicationFinder finder,
            WorkspaceReferencePort workspaces,
            TagManager<ApplicationTag, Application> tags,
            ApplicationNormalizer normalizer,
            ApplicationValidator validator,
            ApplicationMapper mapper) {
        this.repository = repository;
        this.finder = finder;
        this.workspaces = workspaces;
        this.tags = tags;
        this.normalizer = normalizer;
        this.validator = validator;
        this.mapper = mapper;
    }

    @ResourceVisibility(Application.class)
    @Transactional(readOnly = true)
    public ApplicationScopeReferenceOutput findActiveScope(
            String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application application = finder.findActive(identifier, workspaceId);
        return mapper.toScopeReference(workspaceId, application);
    }

    /**
     * Cross-application access only after Shared has verified the contract or participation scope.
     */
    @Transactional(readOnly = true)
    public ApplicationScopeReferenceOutput findActiveScopeForShared(
            Long workspaceId, Long applicationId) {
        ApplicationScopeReference application =
                repository
                        .findActiveScope(workspaceId, applicationId)
                        .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND));
        if (workspaces.findActiveIdentifiersForResourceSharing(List.of(workspaceId)).isEmpty()) {
            throw new NotFoundException(ApplicationMessageKeys.NOT_FOUND);
        }
        return mapper.toScopeReference(workspaceId, applicationId, application);
    }

    @ResourceVisibility(Application.class)
    @Transactional
    public ApplicationScopeReferenceOutput findActiveScopeForUpdate(
            String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application application = finder.findActiveForUpdate(identifier, workspaceId);
        return mapper.toScopeReference(workspaceId, application);
    }

    @Transactional(readOnly = true)
    public String findIdentifierByInternalId(Long workspaceId, Long applicationId) {
        return repository
                .findIdentifier(workspaceId, applicationId)
                .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND));
    }

    /** Resolves persisted Shared references, including inactive applications and workspaces. */
    @Transactional(readOnly = true)
    public Map<Long, ApplicationReferenceOutput> findReferencesForResourceSharing(
            Collection<Long> applicationIds) {
        if (applicationIds.isEmpty()) return Map.of();
        List<ApplicationReference> referencedApplications =
                repository.findReferences(applicationIds, null);
        Map<Long, String> workspaceIdentifiers =
                workspaces.findReferenceIdentifiersForResourceSharing(
                        referencedApplications.stream()
                                .map(ApplicationReference::getWorkspaceId)
                                .distinct()
                                .toList());
        return referencedApplications.stream()
                .collect(
                        Collectors.toMap(
                                ApplicationReference::getId,
                                application ->
                                        mapper.toReference(
                                                application,
                                                workspaceIdentifiers.get(
                                                        application.getWorkspaceId()))));
    }

    /** Shared discovery resolves application and workspace activity in bulk. */
    @Transactional(readOnly = true)
    public List<ApplicationReferenceOutput> findActiveReferencesForResourceSharing(
            Collection<Long> applicationIds) {
        if (applicationIds.isEmpty()) return List.of();
        List<ApplicationReference> activeApplications =
                repository.findReferences(applicationIds, LifecycleTypeCode.active().value());
        Map<Long, String> activeWorkspaces =
                workspaces.findActiveIdentifiersForResourceSharing(
                        activeApplications.stream()
                                .map(ApplicationReference::getWorkspaceId)
                                .distinct()
                                .toList());
        return activeApplications.stream()
                .filter(application -> activeWorkspaces.containsKey(application.getWorkspaceId()))
                .map(
                        application ->
                                mapper.toReference(
                                        application,
                                        activeWorkspaces.get(application.getWorkspaceId())))
                .toList();
    }

    @ResourceVisibility(Application.class)
    @Transactional(readOnly = true)
    public ApplicationOutput findByIdentifier(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application application = finder.findActive(identifier, workspaceId);
        return mapper.toOutput(application, workspaceIdentifier, tags.findManual(application));
    }

    @ResourceVisibility(Application.class)
    @Transactional(readOnly = true)
    public Page<ApplicationOutput> findAll(
            String workspaceIdentifier, String lifecycle, String tagName, Pageable pageable) {
        validator.validatePagination(pageable);
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        String lifecycleCode = LifecycleFilter.resolve(lifecycle).value();
        String normalizedTag = normalizer.normalizeTag(tagName);
        Sort sort =
                pageable.getSort().getOrderFor("id") == null
                        ? pageable.getSort().and(Sort.by("id"))
                        : pageable.getSort();
        Page<Application> applications =
                repository.findFiltered(
                        workspaceId,
                        lifecycleCode,
                        normalizedTag == null ? null : TagName.of(normalizedTag),
                        PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort));
        Map<String, List<String>> manualTags =
                tags.findManualByOwnerKeys(
                        applications.getContent().stream()
                                .map(Application::getIdentifier)
                                .toList());
        return applications.map(
                application ->
                        mapper.toOutput(
                                application,
                                workspaceIdentifier,
                                manualTags.getOrDefault(application.getIdentifier(), List.of())));
    }

    @ResourceVisibility(Application.class)
    @Transactional(readOnly = true)
    public List<ApplicationSummary> summary(String workspaceIdentifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        return repository.findActiveSummaries(workspaceId).stream().map(mapper::toSummary).toList();
    }
}
