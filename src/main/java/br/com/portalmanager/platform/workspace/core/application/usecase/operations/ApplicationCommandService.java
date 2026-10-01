package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.authorization.annotation.ResourceVisibility;
import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.library.schemavalidation.annotation.SchemaPayload;
import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchema;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationSystemTags;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationTag;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.validation.ApplicationValidator;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.foundation.catalog.applicationscopetype.domain.ApplicationScopeTypeCode;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferenceResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ApplicationCommandService {
    private final ApplicationRepository repository;
    private final ApplicationFinder finder;
    private final WorkspaceReferenceResolver workspaces;
    private final ApplicationNormalizer normalizer;
    private final ApplicationValidator validator;
    private final TagManager<ApplicationTag, Application, Long, String> tags;
    private final WorkspaceQueryService workspaceVisibility;

    public ApplicationCommandService(ApplicationRepository repository, ApplicationFinder finder,
                                     WorkspaceReferenceResolver workspaces, ApplicationNormalizer normalizer,
                                     ApplicationValidator validator,
                                     TagManager<ApplicationTag, Application, Long, String> tags,
                                     WorkspaceQueryService workspaceVisibility) {
        this.repository = repository;
        this.finder = finder;
        this.workspaces = workspaces;
        this.normalizer = normalizer;
        this.validator = validator;
        this.tags = tags;
        this.workspaceVisibility = workspaceVisibility;
    }

    @Transactional
    @ValidateResourceSchema(type = "APPLICATION", code = "application")
    public ApplicationOutput create(String workspaceIdentifier, CreateApplicationInput raw, @SchemaPayload Map<String, Object> schemaPayload) {
        workspaceVisibility.findByIdentifier(workspaceIdentifier);
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        CreateApplicationInput input = normalizer.normalize(raw);
        boolean duplicate = input != null && input.name() != null &&
                repository.existsByWorkspaceIdAndName(workspaceId, input.name());
        validator.validateForCreate(workspaces.resolveWorkspaceType(workspaceIdentifier), input, duplicate);
        Application app = new Application(workspaceId, input.name(), input.alias(), input.acronym(),
                ApplicationScopeTypeCode.of(input.applicationScope()), input.authorizerGroup(), input.settings(), LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        tags.reconcile(saved, input.tags(), ApplicationSystemTags.resolve(saved, workspaceIdentifier));
        return ApplicationOutput.from(saved, workspaceIdentifier, tags.findManual(saved));
    }

    @ResourceVisibility(Application.class)
    @Transactional
    @ValidateResourceSchema(type = "APPLICATION", code = "application")
    public ApplicationOutput update(String workspaceIdentifier, String identifier, UpdateApplicationInput raw, @SchemaPayload Map<String, Object> schemaPayload) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findByIdentifier(identifier, workspaceId);
        ApplicationValidator.requireActive(app);
        UpdateApplicationInput input = normalizer.normalize(raw);
        boolean duplicate = input != null && input.name() != null &&
                repository.existsByWorkspaceIdAndNameAndIdNot(workspaceId, input.name(), app.getId());
        validator.validateForUpdate(workspaces.resolveWorkspaceType(workspaceIdentifier), input, duplicate);
        ApplicationValidator.requireVersion(app.getVersion(), input.version());
        app.update(input.name(), input.alias(), input.acronym(), ApplicationScopeTypeCode.of(input.applicationScope()),
                input.authorizerGroup(), input.settings(), LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        tags.reconcile(saved, input.tags(), ApplicationSystemTags.resolve(saved, workspaceIdentifier));
        return ApplicationOutput.from(saved, workspaceIdentifier, tags.findManual(saved));
    }

    @ResourceVisibility(Application.class)
    @Transactional
    public void inactivate(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findByIdentifier(identifier, workspaceId);
        ApplicationValidator.requireActive(app);
        app.inactivate(LocalDateTime.now());
        repository.saveAndFlush(app);
    }

    @ResourceVisibility(Application.class)
    @Transactional
    public ApplicationOutput restore(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findByIdentifier(identifier, workspaceId);
        ApplicationValidator.requireRestorable(app);
        List<String> manualTags = tags.findManual(app);
        app.restore(LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        tags.reconcile(saved, manualTags, ApplicationSystemTags.resolve(saved, workspaceIdentifier));
        return ApplicationOutput.from(saved, workspaceIdentifier, tags.findManual(saved));
    }

    @ResourceVisibility(Application.class)
    @Transactional
    public void delete(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findByIdentifier(identifier, workspaceId);
        ApplicationValidator.requireDeletable(app);
        app.quarantine(LocalDateTime.now());
        repository.saveAndFlush(app);
    }
}
