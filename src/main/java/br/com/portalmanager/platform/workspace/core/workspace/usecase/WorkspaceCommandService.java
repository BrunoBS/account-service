package br.com.portalmanager.platform.workspace.core.workspace.usecase;

import br.com.portalmanager.platform.workspace.core.workspace.domain.Workspace;
import br.com.portalmanager.platform.workspace.core.workspace.repository.WorkspaceRepository;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.CreateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.UpdateWorkspaceInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.WorkspaceOutput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.support.WorkspaceFinder;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.support.WorkspaceNormalizer;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.support.WorkspaceTaggingSupport;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.validation.WorkspaceValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.workspacetype.domain.WorkspaceTypeCode;
import br.com.portalmanager.platform.messaging.exception.ResourceVersionConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class WorkspaceCommandService {
    private final WorkspaceRepository repository;
    private final WorkspaceFinder finder;
    private final WorkspaceNormalizer normalizer;
    private final WorkspaceValidator validator;
    private final WorkspaceTaggingSupport taggingSupport;

    public WorkspaceCommandService(WorkspaceRepository repository, WorkspaceFinder finder, WorkspaceNormalizer normalizer,
                                   WorkspaceValidator validator, WorkspaceTaggingSupport taggingSupport) {
        this.repository = repository; this.finder = finder; this.normalizer = normalizer;
        this.validator = validator; this.taggingSupport = taggingSupport;
    }

    @Transactional
    public WorkspaceOutput create(CreateWorkspaceInput rawInput) {
        CreateWorkspaceInput input = normalizer.normalize(rawInput);
        boolean nameDuplicate = input != null && input.name() != null && repository.existsByName(input.name());
        validator.validateForCreate(normalizer.toValidationData(input), nameDuplicate);
        LocalDateTime now = LocalDateTime.now();
        Workspace workspace = new Workspace(WorkspaceTypeCode.of(input.workspaceType()), input.name(), input.description(),
                input.requester(), input.acronym(), input.settings(), input.authorizerGroup(), input.emailGroup(), now);
        input.approvers().forEach(a -> workspace.addApprover(a.functional(), a.email()));
        Workspace saved = repository.saveAndFlush(workspace);
        taggingSupport.reconcile(saved, input.tags());
        return WorkspaceOutput.from(saved, taggingSupport.findManual(saved));
    }

    @Transactional
    public WorkspaceOutput update(String identifier, UpdateWorkspaceInput rawInput) {
        Workspace workspace = finder.findActive(identifier);
        UpdateWorkspaceInput input = normalizer.normalize(rawInput);
        boolean nameDuplicate = input != null && input.name() != null && repository.existsByNameAndIdNot(input.name(), workspace.getId());
        validator.validateForUpdate(normalizer.toValidationData(input), nameDuplicate);
        if (!Objects.equals(workspace.getVersion(), input.version())) throw new ResourceVersionConflictException();
        workspace.update(WorkspaceTypeCode.of(input.workspaceType()), input.name(), input.description(), input.requester(),
                input.acronym(), input.settings(), input.authorizerGroup(), input.emailGroup(), LocalDateTime.now());
        workspace.clearApprovers();
        repository.deleteApproversByWorkspaceId(workspace.getId());
        input.approvers().forEach(a -> workspace.addApprover(a.functional(), a.email()));
        Workspace saved = repository.saveAndFlush(workspace);
        taggingSupport.reconcile(saved, input.tags());
        return WorkspaceOutput.from(saved, taggingSupport.findManual(saved));
    }

    @Transactional public void inactivate(String identifier) {
        Workspace workspace = finder.findActive(identifier);
        workspace.inactivate(LocalDateTime.now());
        repository.saveAndFlush(workspace);
    }

    @Transactional public WorkspaceOutput restore(String identifier) {
        Workspace workspace = finder.findInactiveForRestore(identifier);
        List<String> manualTags = taggingSupport.findManual(workspace);
        workspace.restore(LocalDateTime.now());
        Workspace saved = repository.saveAndFlush(workspace);
        taggingSupport.reconcile(saved, manualTags);
        return WorkspaceOutput.from(saved, taggingSupport.findManual(saved));
    }

    @Transactional public void delete(String identifier) {
        Workspace workspace = finder.findInactiveForDeletion(identifier);
        workspace.quarantine(LocalDateTime.now());
        repository.saveAndFlush(workspace);
    }
}
