package br.com.portalmanager.platform.workspace.core.workspace.usecase.operations;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceApprover;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceDetails;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceSystemTags;
import br.com.portalmanager.platform.workspace.core.workspace.domain.WorkspaceTag;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.mapping.WorkspaceMapper;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.ApproverInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.CreateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.UpdateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeCode;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeEnum;
import br.com.portalmanager.platform.workspace.foundation.integration.PhaseCompletionPort;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceCommandService {

    private final WorkspaceRepository repository;
    private final WorkspaceFinder finder;
    private final WorkspaceValidator validator;
    private final WorkspaceMapper mapper;
    private final TagManager<WorkspaceTag, Workspace> tags;
    private final PhaseCompletionPort phaseCompletion;

    public WorkspaceCommandService(
        WorkspaceRepository repository,
        WorkspaceFinder finder,
        WorkspaceValidator validator,
        WorkspaceMapper mapper,
        TagManager<WorkspaceTag, Workspace> tags,
        PhaseCompletionPort phaseCompletion
    ) {
        this.repository = repository;
        this.finder = finder;
        this.validator = validator;
        this.mapper = mapper;
        this.tags = tags;
        this.phaseCompletion = phaseCompletion;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "WORKSPACE_CREATED", resourceType = "WORKSPACE")
    public WorkspaceOutput create(CreateWorkspaceInput input) {
        validator.validateForCreate(input);
        WorkspaceDetails details = mapper.toDetails(input);
        Workspace workspace = new Workspace(details, LocalDateTime.now());
        input.approvers().forEach(approver -> workspace.addApprover(approver.functional(), approver.email()));
        Workspace savedWorkspace = repository.saveAndFlush(workspace);
        List<String> manualTags = tags.createInitialTags(
            savedWorkspace,
            input.tags(),
            WorkspaceSystemTags.resolve(savedWorkspace)
        );
        if (WorkspaceTypeEnum.MANAGER.name().equals(savedWorkspace.getWorkspaceType().value())) {
            phaseCompletion.registerPhaseCompletion(savedWorkspace.getId(), OnboardingPhaseTypeCode.workspaceRegistration());
        }
        return mapper.toOutput(savedWorkspace, manualTags);
    }

    @Transactional
    public void completeOnboarding(String identifier) {
        Workspace workspace = finder.findActiveForUpdate(identifier);
        workspace.completeOnboarding(LocalDateTime.now());
        repository.saveAndFlush(workspace);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "WORKSPACE_UPDATED", resourceType = "WORKSPACE")
    public WorkspaceOutput update(String identifier, UpdateWorkspaceInput input) {
        Workspace workspace = finder.findActiveForUpdate(identifier);
        validator.validateForUpdate(workspace, input);

        WorkspaceDetails details = mapper.toDetails(input);
        workspace.update(details, LocalDateTime.now());
        reconcileApprovers(workspace, input.approvers());

        Workspace savedWorkspace = repository.saveAndFlush(workspace);
        List<String> manualTags = tags.reconcileAndReturnManualTags(
            savedWorkspace,
            input.tags(),
            WorkspaceSystemTags.resolve(savedWorkspace)
        );
        return mapper.toOutput(savedWorkspace, manualTags);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "WORKSPACE_DEACTIVATED", resourceType = "WORKSPACE")
    public WorkspaceOutput inactivate(String identifier) {
        Workspace workspace = finder.findActive(identifier);
        workspace.inactivate(LocalDateTime.now());
        Workspace savedWorkspace = repository.saveAndFlush(workspace);
        return mapper.toOutput(savedWorkspace, tags.findManual(savedWorkspace));
    }

    @Transactional
    @Auditable(action = AuditAction.RESTORE, event = "WORKSPACE_RESTORED", resourceType = "WORKSPACE")
    public WorkspaceOutput restore(String identifier) {
        Workspace workspace = finder.findInactiveForRestore(identifier);
        List<String> manualTags = tags.findManual(workspace);
        workspace.restore(LocalDateTime.now());
        Workspace savedWorkspace = repository.saveAndFlush(workspace);
        return mapper.toOutput(savedWorkspace, manualTags);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "WORKSPACE_DELETED", resourceType = "WORKSPACE")
    public WorkspaceOutput delete(String identifier) {
        Workspace workspace = finder.findInactiveForDeletion(identifier);
        workspace.quarantine(LocalDateTime.now());
        Workspace savedWorkspace = repository.saveAndFlush(workspace);
        return mapper.toOutput(savedWorkspace, tags.findManual(savedWorkspace));
    }

    private void reconcileApprovers(Workspace workspace, List<ApproverInput> requestedApprovers) {
        Map<ApproverKey, ApproverInput> pendingApprovers = new LinkedHashMap<>();
        for (ApproverInput approver : requestedApprovers) {
            pendingApprovers.put(new ApproverKey(approver.functional(), approver.email()), approver);
        }
        boolean removedApprovers = false;
        for (WorkspaceApprover approver : List.copyOf(workspace.getApprovers())) {
            ApproverKey key = new ApproverKey(approver.getFunctional(), approver.getEmail());
            if (pendingApprovers.remove(key) == null) {
                workspace.removeApprover(approver);
                removedApprovers = true;
            }
        }
        // Release unique functional/email values before inserting replacements (including swaps).
        if (removedApprovers) repository.flush();
        pendingApprovers.values().forEach(approver -> workspace.addApprover(approver.functional(), approver.email()));
    }

    private record ApproverKey(String functional, String email) {}
}
