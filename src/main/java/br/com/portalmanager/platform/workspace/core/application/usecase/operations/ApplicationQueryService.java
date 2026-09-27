package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationSummary;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import br.com.portalmanager.platform.workspace.foundation.catalog.lifecycletype.domain.LifecycleTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class ApplicationQueryService {
    private final ApplicationRepository repository;
    private final ApplicationFinder finder;
    private final WorkspaceFinder workspaceFinder;
    private final ApplicationTagManager tags;
    private final ApplicationNormalizer normalizer;

    public ApplicationQueryService(ApplicationRepository repository, ApplicationFinder finder,
                                   WorkspaceFinder workspaceFinder, ApplicationTagManager tags,
                                   ApplicationNormalizer normalizer) {
        this.repository = repository;
        this.finder = finder;
        this.workspaceFinder = workspaceFinder;
        this.tags = tags;
        this.normalizer = normalizer;
    }

    @ResourceVisibility
    @Transactional(readOnly = true)
    public ApplicationOutput findByIdentifier(String workspaceIdentifier, String identifier) {
        Workspace workspace = workspaceFinder.findActive(workspaceIdentifier);
        Application app = finder.findActive(identifier, workspace.getId());
        return ApplicationOutput.from(app, tags.findManual(app));
    }

    @ResourceVisibility
    @Transactional(readOnly = true)
    public List<ApplicationOutput> findAll(String workspaceIdentifier, Boolean active, String tagName) {
        Workspace workspace = workspaceFinder.findActive(workspaceIdentifier);
        String lifecycle = Boolean.FALSE.equals(active) ? LifecycleTypeCode.inactive().value() : LifecycleTypeCode.active().value();
        String tag = normalizer.normalizeTag(tagName);
        List<Application> applications;
        if (tag == null || tag.isBlank()) applications = repository.findByWorkspaceAndLifecycle(workspace.getId(), lifecycle);
        else {
            List<String> identifiers = tags.findIdentifiersByTag(tag);
            if (identifiers.isEmpty()) return List.of();
            applications = repository.findByWorkspaceLifecycleAndIdentifiers(workspace.getId(), lifecycle, identifiers);
        }
        Map<String, List<String>> manual = tags.findManualByIds(applications.stream().map(Application::getIdentifier).toList());
        return applications.stream().map(app -> ApplicationOutput.from(app,
                manual.getOrDefault(app.getIdentifier(), List.of()))).toList();
    }

    @ResourceVisibility
    @Transactional(readOnly = true)
    public List<ApplicationSummary> summary(String workspaceIdentifier) {
        Workspace workspace = workspaceFinder.findActive(workspaceIdentifier);
        return repository.findByWorkspaceAndLifecycle(workspace.getId(), LifecycleTypeCode.active().value())
                .stream().map(ApplicationSummary::from).toList();
    }
}
