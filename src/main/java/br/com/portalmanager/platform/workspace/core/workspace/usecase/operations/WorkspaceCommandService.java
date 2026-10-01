package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.library.schemavalidation.annotation.SchemaPayload;
import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchema;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceSystemTags;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceTag;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.ApproverInput;
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
    private final WorkspaceValidator validator;
    private final TagManager<WorkspaceTag, Workspace, Long, String> tags;

    public WorkspaceCommandService(WorkspaceRepository repository, WorkspaceFinder finder,
                                   WorkspaceValidator validator,
                                   TagManager<WorkspaceTag, Workspace, Long, String> tags) {
        this.repository = repository;
        this.finder = finder;
        this.validator = validator;
        this.tags = tags;
    }

    @Transactional
    @ValidateResourceSchema(type = "WORKSPACE", code = "workspace")
    public WorkspaceOutput create(@SchemaPayload CreateWorkspaceInput input) {
        boolean nameDuplicate = repository.existsByName(input.name());
        validator.validateForCreate(input.version(), input.workspaceType(), input.approvers(), nameDuplicate);
        LocalDateTime now = LocalDateTime.now();
        WorkspaceTypeCode workspaceType = WorkspaceTypeCode.of(input.workspaceType());
        Workspace workspace = new Workspace(
                workspaceType,
                input.name(),
                input.description(),
                input.requester(),
                input.acronym(),
                input.settings(),
                input.authorizerGroup(),
                input.emailGroup(),
                now
        );
        input.approvers().forEach(a -> workspace.addApprover(a.functional(), a.email()));
        Workspace saved = repository.saveAndFlush(workspace);
        tags.reconcile(saved, input.tags(), WorkspaceSystemTags.resolve(saved));
        return WorkspaceOutput.from(saved, tags.findManual(saved));
    }

    @Transactional
    @ValidateResourceSchema(type = "WORKSPACE", code = "workspace")
    public WorkspaceOutput update(String identifier, @SchemaPayload UpdateWorkspaceInput input) {
        Workspace workspace = finder.findByIdentifier(identifier);
        validator.requireActive(workspace);
        boolean nameDuplicate = repository.existsByNameAndIdNot(input.name(), workspace.getId());
        validator.validateForUpdate(input.workspaceType(), input.approvers(), nameDuplicate);
        validator.requireVersion(workspace.getVersion(), input.version());
        WorkspaceTypeCode workspaceType = WorkspaceTypeCode.of(input.workspaceType());
        workspace.update(
                workspaceType,
                input.name(),
                input.description(),
                input.requester(),
                input.acronym(),
                input.settings(),
                input.authorizerGroup(),
                input.emailGroup(),
                LocalDateTime.now()
        );
        replaceApprovers(workspace, input.approvers());
        Workspace saved = repository.saveAndFlush(workspace);
        tags.reconcile(saved, input.tags(), WorkspaceSystemTags.resolve(saved));
        return WorkspaceOutput.from(saved, tags.findManual(saved));
    }

    private void replaceApprovers(Workspace workspace, List<ApproverInput> approvers) {
        workspace.clearApprovers();
        repository.deleteApproversByWorkspaceId(workspace.getId());
        approvers.forEach(approver -> workspace.addApprover(approver.functional(), approver.email()));
    }

    @Transactional
    public void inactivate(String identifier) {
        Workspace workspace = finder.findByIdentifier(identifier);
        validator.requireActive(workspace);
        workspace.inactivate(LocalDateTime.now());
        repository.saveAndFlush(workspace);
    }

    @Transactional
    public WorkspaceOutput restore(String identifier) {
        Workspace workspace = finder.findByIdentifier(identifier);
        validator.requireRestorable(workspace);
        List<String> manualTags = tags.findManual(workspace);
        workspace.restore(LocalDateTime.now());
        Workspace saved = repository.saveAndFlush(workspace);
        tags.reconcile(saved, manualTags, WorkspaceSystemTags.resolve(saved));
        return WorkspaceOutput.from(saved, tags.findManual(saved));
    }

    @Transactional
    public void delete(String identifier) {
        Workspace workspace = finder.findByIdentifier(identifier);
        validator.requireDeletable(workspace);
        workspace.quarantine(LocalDateTime.now());
        repository.saveAndFlush(workspace);
    }
}
