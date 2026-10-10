package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceSystemTags;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceTag;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.CreateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.UpdateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceCommandService {

    private final WorkspaceRepository repository;
    private final WorkspaceFinder finder;
    private final WorkspaceNormalizer normalizer;
    private final WorkspaceValidator validator;
    private final TagManager<WorkspaceTag, Workspace> tags;

    public WorkspaceCommandService(
        WorkspaceRepository repository,
        WorkspaceFinder finder,
        WorkspaceNormalizer normalizer,
        WorkspaceValidator validator,
        TagManager<WorkspaceTag, Workspace> tags
    ) {
        this.repository = repository;
        this.finder = finder;
        this.normalizer = normalizer;
        this.validator = validator;
        this.tags = tags;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "WORKSPACE_CREATED", resourceType = "WORKSPACE")
    public WorkspaceOutput create(CreateWorkspaceInput rawInput) {
        CreateWorkspaceInput input = normalizer.normalize(rawInput);
        boolean nameDuplicate = input != null && input.name() != null && repository.existsByName(input.name());
        validator.validateForCreate(normalizer.toValidationData(input), nameDuplicate);
        LocalDateTime now = LocalDateTime.now();
        Workspace workspace = new Workspace(
            WorkspaceTypeCode.of(input.workspaceType()),
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
    @Auditable(action = AuditAction.UPDATE, event = "WORKSPACE_UPDATED", resourceType = "WORKSPACE")
    public WorkspaceOutput update(String identifier, UpdateWorkspaceInput rawInput) {
        Workspace workspace = finder.findActive(identifier);
        UpdateWorkspaceInput input = normalizer.normalize(rawInput);
        boolean nameDuplicate =
            input != null && input.name() != null && repository.existsByNameAndIdNot(input.name(), workspace.getId());
        validator.validateForUpdate(normalizer.toValidationData(input), nameDuplicate);
        WorkspaceValidator.requireVersion(workspace.getVersion(), input.version());
        workspace.update(
            WorkspaceTypeCode.of(input.workspaceType()),
            input.name(),
            input.description(),
            input.requester(),
            input.acronym(),
            input.settings(),
            input.authorizerGroup(),
            input.emailGroup(),
            LocalDateTime.now()
        );
        workspace.clearApprovers();
        repository.deleteApproversByWorkspaceId(workspace.getId());
        input.approvers().forEach(a -> workspace.addApprover(a.functional(), a.email()));
        Workspace saved = repository.saveAndFlush(workspace);
        tags.reconcile(saved, input.tags(), WorkspaceSystemTags.resolve(saved));
        return WorkspaceOutput.from(saved, tags.findManual(saved));
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "WORKSPACE_DEACTIVATED", resourceType = "WORKSPACE")
    public WorkspaceOutput inactivate(String identifier) {
        Workspace workspace = finder.findActive(identifier);
        workspace.inactivate(LocalDateTime.now());
        Workspace saved = repository.saveAndFlush(workspace);
        return WorkspaceOutput.from(saved, tags.findManual(saved));
    }

    @Transactional
    @Auditable(action = AuditAction.RESTORE, event = "WORKSPACE_RESTORED", resourceType = "WORKSPACE")
    public WorkspaceOutput restore(String identifier) {
        Workspace workspace = finder.findInactiveForRestore(identifier);
        List<String> manualTags = tags.findManual(workspace);
        workspace.restore(LocalDateTime.now());
        Workspace saved = repository.saveAndFlush(workspace);
        tags.reconcile(saved, manualTags, WorkspaceSystemTags.resolve(saved));
        return WorkspaceOutput.from(saved, tags.findManual(saved));
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "WORKSPACE_DELETED", resourceType = "WORKSPACE")
    public WorkspaceOutput delete(String identifier) {
        Workspace workspace = finder.findInactiveForDeletion(identifier);
        workspace.quarantine(LocalDateTime.now());
        Workspace saved = repository.saveAndFlush(workspace);
        return WorkspaceOutput.from(saved, tags.findManual(saved));
    }
}
