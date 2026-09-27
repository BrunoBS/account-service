package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.validation.ApplicationValidator;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceFinder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationCommandService {
    private final ApplicationRepository repository;
    private final ApplicationFinder finder;
    private final WorkspaceFinder workspaceFinder;
    private final ApplicationNormalizer normalizer;
    private final ApplicationValidator validator;
    private final ApplicationTagManager tags;

    public ApplicationCommandService(ApplicationRepository repository, ApplicationFinder finder,
                                     WorkspaceFinder workspaceFinder, ApplicationNormalizer normalizer,
                                     ApplicationValidator validator, ApplicationTagManager tags) {
        this.repository = repository;
        this.finder = finder;
        this.workspaceFinder = workspaceFinder;
        this.normalizer = normalizer;
        this.validator = validator;
        this.tags = tags;
    }

    @Transactional
    public ApplicationOutput create(String workspaceIdentifier, CreateApplicationInput raw) {
        Workspace workspace = workspaceFinder.findActive(workspaceIdentifier);
        CreateApplicationInput input = normalizer.normalize(raw);
        boolean duplicate = input != null && input.name() != null &&
                repository.existsByWorkspace_IdAndName(workspace.getId(), input.name());
        validator.validateForCreate(workspace, input, duplicate);
        Application app = new Application(workspace, input.name(), input.alias(), input.acronym(),
                input.applicationScope(), input.authorizerGroup(), input.settings(), input.isDefault(), LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        tags.reconcile(saved, input.tags());
        return ApplicationOutput.from(saved, tags.findManual(saved));
    }

    @Transactional
    public ApplicationOutput update(String workspaceIdentifier, String identifier, UpdateApplicationInput raw) {
        Workspace workspace = workspaceFinder.findActive(workspaceIdentifier);
        Application app = finder.findActive(identifier, workspace.getId());
        UpdateApplicationInput input = normalizer.normalize(raw);
        boolean duplicate = input != null && input.name() != null &&
                repository.existsByWorkspace_IdAndNameAndIdNot(workspace.getId(), input.name(), app.getId());
        validator.validateForUpdate(workspace, input, duplicate);
        validator.requireVersion(app.getVersion(), input.version());
        app.update(input.name(), input.alias(), input.acronym(), input.applicationScope(),
                input.authorizerGroup(), input.settings(), input.isDefault(), LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        tags.reconcile(saved, input.tags());
        return ApplicationOutput.from(saved, tags.findManual(saved));
    }

    @Transactional
    public void inactivate(String workspaceIdentifier, String identifier) {
        Workspace workspace = workspaceFinder.findActive(workspaceIdentifier);
        Application app = finder.findActive(identifier, workspace.getId());
        app.inactivate(LocalDateTime.now());
        repository.saveAndFlush(app);
    }

    @Transactional
    public ApplicationOutput restore(String workspaceIdentifier, String identifier) {
        Workspace workspace = workspaceFinder.findActive(workspaceIdentifier);
        Application app = finder.findInactive(identifier, workspace.getId());
        List<String> manualTags = tags.findManual(app);
        app.restore(LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        tags.reconcile(saved, manualTags);
        return ApplicationOutput.from(saved, tags.findManual(saved));
    }
}
