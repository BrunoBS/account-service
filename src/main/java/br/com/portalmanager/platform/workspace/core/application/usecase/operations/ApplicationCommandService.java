package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.validation.ApplicationValidator;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationCommandService {
    private final ApplicationRepository repository;
    private final ApplicationFinder finder;
    private final WorkspaceReferenceResolver workspaces;
    private final ApplicationNormalizer normalizer;
    private final ApplicationValidator validator;
    private final ApplicationTagManager tags;

    public ApplicationCommandService(ApplicationRepository repository, ApplicationFinder finder,
                                     WorkspaceReferenceResolver workspaces, ApplicationNormalizer normalizer,
                                     ApplicationValidator validator, ApplicationTagManager tags) {
        this.repository = repository;
        this.finder = finder;
        this.workspaces = workspaces;
        this.normalizer = normalizer;
        this.validator = validator;
        this.tags = tags;
    }

    @Transactional
    public ApplicationOutput create(String workspaceIdentifier, CreateApplicationInput raw) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        CreateApplicationInput input = normalizer.normalize(raw);
        boolean duplicate = input != null && input.name() != null &&
                repository.existsByWorkspaceIdAndName(workspaceId, input.name());
        validator.validateForCreate(workspaces.resolveWorkspaceType(workspaceIdentifier), input, duplicate);
        Application app = new Application(workspaceId, input.name(), input.alias(), input.acronym(),
                input.applicationScope(), input.authorizerGroup(), input.settings(), input.isDefault(), LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        tags.reconcile(saved, workspaceIdentifier, input.tags());
        return ApplicationOutput.from(saved, workspaceIdentifier, tags.findManual(saved));
    }

    @Transactional
    public ApplicationOutput update(String workspaceIdentifier, String identifier, UpdateApplicationInput raw) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findActive(identifier, workspaceId);
        UpdateApplicationInput input = normalizer.normalize(raw);
        boolean duplicate = input != null && input.name() != null &&
                repository.existsByWorkspaceIdAndNameAndIdNot(workspaceId, input.name(), app.getId());
        validator.validateForUpdate(workspaces.resolveWorkspaceType(workspaceIdentifier), input, duplicate);
        validator.requireVersion(app.getVersion(), input.version());
        app.update(input.name(), input.alias(), input.acronym(), input.applicationScope(),
                input.authorizerGroup(), input.settings(), input.isDefault(), LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        tags.reconcile(saved, workspaceIdentifier, input.tags());
        return ApplicationOutput.from(saved, workspaceIdentifier, tags.findManual(saved));
    }

    @Transactional
    public void inactivate(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findActive(identifier, workspaceId);
        app.inactivate(LocalDateTime.now());
        repository.saveAndFlush(app);
    }

    @Transactional
    public ApplicationOutput restore(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findInactive(identifier, workspaceId);
        List<String> manualTags = tags.findManual(app);
        app.restore(LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        tags.reconcile(saved, workspaceIdentifier, manualTags);
        return ApplicationOutput.from(saved, workspaceIdentifier, tags.findManual(saved));
    }
}
