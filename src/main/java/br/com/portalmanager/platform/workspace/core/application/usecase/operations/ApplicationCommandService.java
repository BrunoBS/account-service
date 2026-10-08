package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.schemavalidation.annotation.SchemaPayload;
import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchema;
import br.com.portalmanager.platform.library.tagging.TagManager;
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

@Service
public class ApplicationCommandService {
    private final ApplicationRepository repository;
    private final ApplicationFinder finder;
    private final WorkspaceReferenceResolver workspaces;
    private final ApplicationNormalizer normalizer;
    private final ApplicationValidator validator;
    private final TagManager<ApplicationTag, Application> tags;
    private final ApplicationQueryService visibility;
    private final WorkspaceQueryService workspaceVisibility;

    public ApplicationCommandService(ApplicationRepository repository, ApplicationFinder finder,
                                     WorkspaceReferenceResolver workspaces, ApplicationNormalizer normalizer,
                                     ApplicationValidator validator,
                                     TagManager<ApplicationTag, Application> tags,
                                     ApplicationQueryService visibility, WorkspaceQueryService workspaceVisibility) {
        this.repository = repository;
        this.finder = finder;
        this.workspaces = workspaces;
        this.normalizer = normalizer;
        this.validator = validator;
        this.tags = tags;
        this.visibility = visibility;
        this.workspaceVisibility = workspaceVisibility;
    }

    @Transactional
    @ValidateResourceSchema(type = "APPLICATION", code = "application")
    @Auditable(action = AuditAction.CREATE, event = "APPLICATION_CREATED", resourceType = "APPLICATION")
    public ApplicationOutput create(String workspaceIdentifier, @SchemaPayload CreateApplicationInput raw) {
        workspaceVisibility.findByIdentifier(workspaceIdentifier);
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        CreateApplicationInput input = normalizer.normalize(raw);
        boolean duplicate = input != null && input.name() != null &&
                repository.existsByWorkspaceIdAndName(workspaceId, input.name());
        validator.validateForCreate(workspaces.resolveWorkspaceType(workspaceIdentifier), input, duplicate);
        Application app = new Application(workspaceId, input.name(), input.alias(), input.acronym(),
                ApplicationScopeTypeCode.of(input.applicationScope()), input.authorizerGroup(), settings(input.settings()), LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        tags.reconcile(saved, input.tags(), ApplicationSystemTags.resolve(saved, workspaceIdentifier));
        return ApplicationOutput.from(saved, workspaceIdentifier, tags.findManual(saved));
    }

    @Transactional
    @ValidateResourceSchema(type = "APPLICATION", code = "application")
    @Auditable(action = AuditAction.UPDATE, event = "APPLICATION_UPDATED", resourceType = "APPLICATION")
    public ApplicationOutput update(String workspaceIdentifier, String identifier,
                                    @SchemaPayload UpdateApplicationInput raw) {
        visibility.findByIdentifier(workspaceIdentifier, identifier);
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findActive(identifier, workspaceId);
        UpdateApplicationInput input = normalizer.normalize(raw);
        boolean duplicate = input != null && input.name() != null &&
                repository.existsByWorkspaceIdAndNameAndIdNot(workspaceId, input.name(), app.getId());
        validator.validateForUpdate(workspaces.resolveWorkspaceType(workspaceIdentifier), input, duplicate);
        validator.requireVersion(app.getVersion(), input.version());
        app.update(input.name(), input.alias(), input.acronym(), ApplicationScopeTypeCode.of(input.applicationScope()),
                input.authorizerGroup(), settings(input.settings()), LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        tags.reconcile(saved, input.tags(), ApplicationSystemTags.resolve(saved, workspaceIdentifier));
        return ApplicationOutput.from(saved, workspaceIdentifier, tags.findManual(saved));
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "APPLICATION_DEACTIVATED", resourceType = "APPLICATION")
    public ApplicationOutput inactivate(String workspaceIdentifier, String identifier) {
        visibility.findByIdentifier(workspaceIdentifier, identifier);
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findActive(identifier, workspaceId);
        app.inactivate(LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        return ApplicationOutput.from(saved, workspaceIdentifier, tags.findManual(saved));
    }

    @Transactional
    @Auditable(action = AuditAction.RESTORE, event = "APPLICATION_RESTORED", resourceType = "APPLICATION")
    public ApplicationOutput restore(String workspaceIdentifier, String identifier) {
        visibility.findInactiveByIdentifier(workspaceIdentifier, identifier);
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findInactive(identifier, workspaceId);
        List<String> manualTags = tags.findManual(app);
        app.restore(LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        tags.reconcile(saved, manualTags, ApplicationSystemTags.resolve(saved, workspaceIdentifier));
        return ApplicationOutput.from(saved, workspaceIdentifier, tags.findManual(saved));
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "APPLICATION_DELETED", resourceType = "APPLICATION")
    public ApplicationOutput delete(String workspaceIdentifier, String identifier) {
        visibility.findInactiveForDeletion(workspaceIdentifier, identifier);
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application app = finder.findInactiveForDeletion(identifier, workspaceId);
        app.quarantine(LocalDateTime.now());
        Application saved = repository.saveAndFlush(app);
        return ApplicationOutput.from(saved, workspaceIdentifier, tags.findManual(saved));
    }

    private String settings(tools.jackson.databind.JsonNode value) {
        return value == null ? "{}" : value.toString();
    }
}
