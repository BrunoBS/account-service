package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationTag;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationSummary;
import br.com.portalmanager.platform.workspace.core.application.usecase.validation.ApplicationValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ApplicationQueryService {
    private final ApplicationRepository repository;
    private final ApplicationFinder finder;
    private final WorkspaceReferenceResolver workspaces;
    private final TagManager<ApplicationTag, Application, Long, String> tags;
    private final ApplicationNormalizer normalizer;

    public ApplicationQueryService(ApplicationRepository repository, ApplicationFinder finder,
                                   WorkspaceReferenceResolver workspaces,
                                   TagManager<ApplicationTag, Application, Long, String> tags,
                                   ApplicationNormalizer normalizer) {
        this.repository = repository;
        this.finder = finder;
        this.workspaces = workspaces;
        this.tags = tags;
        this.normalizer = normalizer;
    }

    @ResourceVisibility(Application.class)
    @Transactional(readOnly = true)
    public ApplicationOutput findByIdentifier(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findByIdentifier(identifier, workspaceId);
        ApplicationValidator.requireActive(app);
        return ApplicationOutput.from(app, workspaceIdentifier, tags.findManual(app));
    }

    @Transactional(readOnly = true)
    public ApplicationOutput findInactiveByIdentifier(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findByIdentifier(identifier, workspaceId);
        ApplicationValidator.requireRestorable(app);
        return ApplicationOutput.from(app, workspaceIdentifier, tags.findManual(app));
    }

    @Transactional(readOnly = true)
    public ApplicationOutput findInactiveForDeletion(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findByIdentifier(identifier, workspaceId);
        ApplicationValidator.requireDeletable(app);
        return ApplicationOutput.from(app, workspaceIdentifier, tags.findManual(app));
    }

    @ResourceVisibility(Application.class)
    @Transactional(readOnly = true)
    public List<ApplicationOutput> findAll(String workspaceIdentifier, Boolean active, String tagName) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        String lifecycle = Boolean.FALSE.equals(active) ? LifecycleTypeCode.inactive().value() : LifecycleTypeCode.active().value();
        String tag = normalizer.normalizeTag(tagName);
        List<Application> applications;
        if (tag == null || tag.isBlank()) applications = repository.findByWorkspaceAndLifecycle(workspaceId, lifecycle);
        else {
            List<String> identifiers = tags.findOwnerKeysByTag(tag);
            if (identifiers.isEmpty()) return List.of();
            applications = repository.findByWorkspaceLifecycleAndIdentifiers(workspaceId, lifecycle, identifiers);
        }
        Map<String, List<String>> manual = tags.findManualByOwnerKeys(applications.stream().map(Application::getIdentifier).toList());
        return applications.stream().map(app -> ApplicationOutput.from(app, workspaceIdentifier,
                manual.getOrDefault(app.getIdentifier(), List.of()))).toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationSummary> summary(String workspaceIdentifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        return repository.findByWorkspaceAndLifecycle(workspaceId, LifecycleTypeCode.active().value())
                .stream().map(ApplicationSummary::from).toList();
    }
}
