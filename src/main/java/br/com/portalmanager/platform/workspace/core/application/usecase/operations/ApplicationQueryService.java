package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.messaging.exception.NotFoundException;
import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationMessageKeys;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationTag;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationScopeReferenceOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationSummary;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationQueryService {

    private final WorkspaceQueryService workspaceQueries;
    private final ApplicationRepository repository;
    private final ApplicationFinder finder;
    private final WorkspaceReferenceResolver workspaces;
    private final TagManager<ApplicationTag, Application> tags;
    private final ApplicationNormalizer normalizer;
    private final EntityManager entityManager;

    public ApplicationQueryService(
        WorkspaceQueryService workspaceQueries,
        ApplicationRepository repository,
        ApplicationFinder finder,
        WorkspaceReferenceResolver workspaces,
        TagManager<ApplicationTag, Application> tags,
        ApplicationNormalizer normalizer,
        EntityManager entityManager
    ) {
        this.workspaceQueries = workspaceQueries;
        this.repository = repository;
        this.finder = finder;
        this.workspaces = workspaces;
        this.tags = tags;
        this.normalizer = normalizer;
        this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public Long findInternalIdByIdentifier(String workspaceIdentifier, String identifier) {
        return repository
            .findByIdentifierAndWorkspaceId(identifier, workspaces.resolveInternalId(workspaceIdentifier))
            .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND))
            .getId();
    }

    @ResourceVisibility(Application.class)
    @Transactional(readOnly = true)
    public ApplicationScopeReferenceOutput findActiveScope(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application application = finder.findActive(identifier, workspaceId);
        return new ApplicationScopeReferenceOutput(
            workspaceId,
            application.getId(),
            application.getApplicationScope().value()
        );
    }

    /** Cross-application access only after Shared has verified the contract or participation scope. */
    @Transactional(readOnly = true)
    public ApplicationScopeReferenceOutput findActiveScopeForShared(Long workspaceId, Long applicationId) {
        Application application = repository
            .findById(applicationId)
            .filter(
                reference ->
                    reference.getWorkspaceId().equals(workspaceId) &&
                    LifecycleTypeCode.active().equals(reference.getLifecycle())
            )
            .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND));
        if (workspaceQueries.findActiveIdentifiersByInternalIds(List.of(workspaceId)).isEmpty()) {
            throw new NotFoundException(ApplicationMessageKeys.NOT_FOUND);
        }
        return new ApplicationScopeReferenceOutput(
            workspaceId,
            applicationId,
            application.getApplicationScope().value()
        );
    }

    @ResourceVisibility(Application.class)
    @Transactional
    public ApplicationScopeReferenceOutput findActiveScopeForUpdate(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application application = repository
            .findByIdentifierAndWorkspaceIdForUpdate(identifier, workspaceId)
            .filter(reference -> LifecycleTypeCode.active().equals(reference.getLifecycle()))
            .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND));
        // Refresh under the lock: visibility checks may have loaded this entity before waiting for another transaction.
        entityManager.refresh(application, LockModeType.PESSIMISTIC_WRITE);
        if (!LifecycleTypeCode.active().equals(application.getLifecycle())) {
            throw new NotFoundException(ApplicationMessageKeys.NOT_FOUND);
        }
        return new ApplicationScopeReferenceOutput(
            workspaceId,
            application.getId(),
            application.getApplicationScope().value()
        );
    }

    @Transactional(readOnly = true)
    public String findIdentifierByInternalId(Long workspaceId, Long applicationId) {
        return repository
            .findById(applicationId)
            .filter(application -> application.getWorkspaceId().equals(workspaceId))
            .orElseThrow(() -> new NotFoundException(ApplicationMessageKeys.NOT_FOUND))
            .getIdentifier();
    }

    /** Resolves persisted Shared references, including inactive applications and workspaces. */
    @Transactional(readOnly = true)
    public Map<Long, ApplicationReferenceOutput> findReferencesForShared(Collection<Long> applicationIds) {
        if (applicationIds.isEmpty()) return Map.of();
        List<Application> referencedApplications = repository.findAllById(applicationIds);
        Map<Long, String> workspaceIdentifiers = workspaceQueries.findReferenceIdentifiersByInternalIds(
            referencedApplications.stream().map(Application::getWorkspaceId).distinct().toList()
        );
        return referencedApplications
            .stream()
            .collect(
                Collectors.toMap(Application::getId, application ->
                    new ApplicationReferenceOutput(
                        application.getId(),
                        application.getWorkspaceId(),
                        application.getIdentifier(),
                        workspaceIdentifiers.get(application.getWorkspaceId()),
                        application.getName()
                    )
                )
            );
    }

    /** Shared discovery resolves application and workspace activity in bulk. */
    @Transactional(readOnly = true)
    public List<ApplicationReferenceOutput> findActiveReferencesForShared(Collection<Long> applicationIds) {
        if (applicationIds.isEmpty()) return List.of();
        List<Application> activeApplications = repository.findByIdInAndLifecycleValue(
            applicationIds,
            LifecycleTypeCode.active().value()
        );
        Map<Long, String> activeWorkspaces = workspaceQueries.findActiveIdentifiersByInternalIds(
            activeApplications.stream().map(Application::getWorkspaceId).distinct().toList()
        );
        return activeApplications
            .stream()
            .filter(application -> activeWorkspaces.containsKey(application.getWorkspaceId()))
            .map(application ->
                new ApplicationReferenceOutput(
                    application.getId(),
                    application.getWorkspaceId(),
                    application.getIdentifier(),
                    activeWorkspaces.get(application.getWorkspaceId()),
                    application.getName()
                )
            )
            .toList();
    }

    @ResourceVisibility(Application.class)
    @Transactional(readOnly = true)
    public ApplicationOutput findByIdentifier(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findActive(identifier, workspaceId);
        return ApplicationOutput.from(app, workspaceIdentifier, tags.findManual(app));
    }

    /** Cross-application Shared read after the caller has validated the owning contract and participation. */
    @Transactional(readOnly = true)
    public ApplicationOutput findActiveForShared(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findActive(identifier, workspaceId);
        return ApplicationOutput.from(app, workspaceIdentifier, tags.findManual(app));
    }

    @Transactional(readOnly = true)
    public ApplicationOutput findInactiveForShared(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findInactive(identifier, workspaceId);
        return ApplicationOutput.from(app, workspaceIdentifier, tags.findManual(app));
    }

    @ResourceVisibility(Application.class)
    @Transactional(readOnly = true)
    public ApplicationOutput findInactiveByIdentifier(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findInactive(identifier, workspaceId);
        return ApplicationOutput.from(app, workspaceIdentifier, tags.findManual(app));
    }

    @ResourceVisibility(Application.class)
    @Transactional(readOnly = true)
    public ApplicationOutput findInactiveForDeletion(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findInactiveForDeletion(identifier, workspaceId);
        return ApplicationOutput.from(app, workspaceIdentifier, tags.findManual(app));
    }

    @ResourceVisibility(Application.class)
    @Transactional(readOnly = true)
    public List<ApplicationOutput> findAll(String workspaceIdentifier, Boolean active, String tagName) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        String lifecycle = Boolean.FALSE.equals(active)
            ? LifecycleTypeCode.inactive().value()
            : LifecycleTypeCode.active().value();
        String tag = normalizer.normalizeTag(tagName);
        List<Application> applications;
        if (tag == null || tag.isBlank()) applications = repository.findByWorkspaceAndLifecycle(workspaceId, lifecycle);
        else {
            List<String> identifiers = tags.findOwnerKeysByTag(tag);
            if (identifiers.isEmpty()) return List.of();
            applications = repository.findByWorkspaceLifecycleAndIdentifiers(workspaceId, lifecycle, identifiers);
        }
        Map<String, List<String>> manual = tags.findManualByOwnerKeys(
            applications.stream().map(Application::getIdentifier).toList()
        );
        return applications
            .stream()
            .map(app ->
                ApplicationOutput.from(app, workspaceIdentifier, manual.getOrDefault(app.getIdentifier(), List.of()))
            )
            .toList();
    }

    @ResourceVisibility(Application.class)
    @Transactional(readOnly = true)
    public List<ApplicationSummary> summary(String workspaceIdentifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        return repository
            .findByWorkspaceAndLifecycle(workspaceId, LifecycleTypeCode.active().value())
            .stream()
            .map(ApplicationSummary::from)
            .toList();
    }
}
