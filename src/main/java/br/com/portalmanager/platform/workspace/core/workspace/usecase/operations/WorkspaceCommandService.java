package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.library.schemavalidation.annotation.SchemaPayload;
import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchema;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceSystemTags;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceTag;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.CreateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.UpdateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class WorkspaceCommandService {
    private final WorkspaceRepository repository;
    private final WorkspaceFinder finder;
    private final WorkspaceNormalizer normalizer;
    private final WorkspaceValidator validator;
    private final TagManager<WorkspaceTag, Workspace, Long, String> tags;

    public WorkspaceCommandService(WorkspaceRepository repository, WorkspaceFinder finder, WorkspaceNormalizer normalizer,
                                   WorkspaceValidator validator,
                                   TagManager<WorkspaceTag, Workspace, Long, String> tags) {
        this.repository = repository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
        this.tags = tags;
    }

    @Transactional
    @ValidateResourceSchema(type = "WORKSPACE", code = "workspace")
    public WorkspaceOutput create(@SchemaPayload CreateWorkspaceInput rawInput) {
        CreateWorkspaceInput input = normalizer.normalize(rawInput);
        boolean nameDuplicate = input != null && input.name() != null && repository.existsByName(input.name());
        validator.validateForCreate(normalizer.toValidationData(input), nameDuplicate);
        LocalDateTime now = LocalDateTime.now();
        Workspace workspace = new Workspace(WorkspaceTypeCode.of(input.workspaceType()), input.name(), input.description(),
                input.requester(), input.acronym(), input.settings() == null ? null : input.settings().toString(), input.authorizerGroup(), input.emailGroup(), now);
        input.approvers().forEach(a -> workspace.addApprover(a.functional(), a.email()));
        Workspace saved = repository.saveAndFlush(workspace);
        tags.reconcile(saved, input.tags(), WorkspaceSystemTags.resolve(saved));
        return WorkspaceOutput.from(saved, tags.findManual(saved));
    }

    @Transactional
    @ValidateResourceSchema(type = "WORKSPACE", code = "workspace")
    public WorkspaceOutput update(String identifier, @SchemaPayload UpdateWorkspaceInput rawInput) {
        Workspace workspace = finder.findByIdentifier(identifier);
        WorkspaceValidator.requireActive(workspace);
        UpdateWorkspaceInput input = normalizer.normalize(rawInput);
        boolean nameDuplicate = input != null && input.name() != null && repository.existsByNameAndIdNot(input.name(), workspace.getId());
        validator.validateForUpdate(normalizer.toValidationData(input), nameDuplicate);
        WorkspaceValidator.requireVersion(workspace.getVersion(), input.version());
        workspace.update(WorkspaceTypeCode.of(input.workspaceType()), input.name(), input.description(), input.requester(),
                input.acronym(), input.settings() == null ? null : input.settings().toString(), input.authorizerGroup(), input.emailGroup(), LocalDateTime.now());
        workspace.clearApprovers();
        repository.deleteApproversByWorkspaceId(workspace.getId());
        input.approvers().forEach(a -> workspace.addApprover(a.functional(), a.email()));
        Workspace saved = repository.saveAndFlush(workspace);
        tags.reconcile(saved, input.tags(), WorkspaceSystemTags.resolve(saved));
        return WorkspaceOutput.from(saved, tags.findManual(saved));
    }

    @Transactional
    public void inactivate(String identifier) {
        Workspace workspace = finder.findByIdentifier(identifier);
        WorkspaceValidator.requireActive(workspace);
        workspace.inactivate(LocalDateTime.now());
        repository.saveAndFlush(workspace);
    }

    @Transactional
    public WorkspaceOutput restore(String identifier) {
        Workspace workspace = finder.findByIdentifier(identifier);
        WorkspaceValidator.requireRestorable(workspace);
        List<String> manualTags = tags.findManual(workspace);
        workspace.restore(LocalDateTime.now());
        Workspace saved = repository.saveAndFlush(workspace);
        tags.reconcile(saved, manualTags, WorkspaceSystemTags.resolve(saved));
        return WorkspaceOutput.from(saved, tags.findManual(saved));
    }

    @Transactional
    public void delete(String identifier) {
        Workspace workspace = finder.findByIdentifier(identifier);
        WorkspaceValidator.requireDeletable(workspace);
        workspace.quarantine(LocalDateTime.now());
        repository.saveAndFlush(workspace);
    }
}
