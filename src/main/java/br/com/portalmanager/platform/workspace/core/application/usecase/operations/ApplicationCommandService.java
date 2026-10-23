package br.com.portalmanager.platform.workspace.core.application.usecase.operations;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.library.tagging.TagManager;
import br.com.portalmanager.platform.workspace.core.application.domain.Application;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationSystemTags;
import br.com.portalmanager.platform.workspace.core.application.domain.ApplicationTag;
import br.com.portalmanager.platform.workspace.core.application.repository.ApplicationRepository;
import br.com.portalmanager.platform.workspace.core.application.usecase.mapping.ApplicationMapper;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.ApplicationOutput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.CreateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.model.UpdateApplicationInput;
import br.com.portalmanager.platform.workspace.core.application.usecase.port.PhaseCompletionPort;
import br.com.portalmanager.platform.workspace.core.application.usecase.port.WorkspaceReferencePort;
import br.com.portalmanager.platform.workspace.core.application.usecase.port.WorkspaceReferencePort.ResolvedWorkspace;
import br.com.portalmanager.platform.workspace.core.application.usecase.validation.ApplicationValidator;
import br.com.portalmanager.platform.workspace.foundation.catalog.onboardingphasetype.domain.OnboardingPhaseTypeCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ApplicationCommandService {

    private final ApplicationRepository repository;
    private final ApplicationFinder finder;
    private final WorkspaceReferencePort workspaces;
    private final ApplicationMapper mapper;
    private final ApplicationValidator validator;
    private final TagManager<ApplicationTag, Application> tags;
    private final PhaseCompletionPort phaseCompletion;

    public ApplicationCommandService(
            ApplicationRepository repository,
            ApplicationFinder finder,
            WorkspaceReferencePort workspaces,
            ApplicationMapper mapper,
            ApplicationValidator validator,
            TagManager<ApplicationTag, Application> tags,
            PhaseCompletionPort phaseCompletion) {
        this.repository = repository;
        this.finder = finder;
        this.workspaces = workspaces;
        this.mapper = mapper;
        this.validator = validator;
        this.tags = tags;
        this.phaseCompletion = phaseCompletion;
    }

    @Transactional
    @Auditable(
            action = AuditAction.CREATE,
            event = "APPLICATION_CREATED",
            resourceType = "APPLICATION")
    public ApplicationOutput create(String workspaceIdentifier, CreateApplicationInput input) {
        ResolvedWorkspace workspace = workspaces.resolveActiveForUpdate(workspaceIdentifier);
        Long workspaceId = workspace.id();
        validator.validateForCreate(workspace.workspaceType(), workspaceId, input);

        boolean firstApplication = !repository.existsByWorkspaceId(workspaceId);
        Application application =
                new Application(workspaceId, mapper.toDetails(input), LocalDateTime.now());
        Application savedApplication = repository.saveAndFlush(application);
        List<String> manualTags =
                tags.createInitialTags(
                        savedApplication,
                        input.tags(),
                        ApplicationSystemTags.resolve(savedApplication, workspaceIdentifier));

        if (firstApplication) {
            phaseCompletion.registerPhaseCompletion(
                    workspaceId, OnboardingPhaseTypeCode.firstApplicationRegistration());
        }
        return mapper.toOutput(savedApplication, workspaceIdentifier, manualTags);
    }

    @Transactional
    @Auditable(
            action = AuditAction.UPDATE,
            event = "APPLICATION_UPDATED",
            resourceType = "APPLICATION")
    public ApplicationOutput update(
            String workspaceIdentifier, String identifier, UpdateApplicationInput input) {
        ResolvedWorkspace workspace = workspaces.resolveActive(workspaceIdentifier);
        Long workspaceId = workspace.id();
        Application application = finder.findActiveForUpdate(identifier, workspaceId);
        validator.validateForUpdate(workspace.workspaceType(), application, input);
        application.update(mapper.toDetails(input), LocalDateTime.now());
        Application savedApplication = repository.saveAndFlush(application);
        List<String> manualTags =
                tags.reconcileAndReturnManualTags(
                        savedApplication,
                        input.tags(),
                        ApplicationSystemTags.resolve(savedApplication, workspaceIdentifier));
        return mapper.toOutput(savedApplication, workspaceIdentifier, manualTags);
    }

    @Transactional
    @Auditable(
            action = AuditAction.DEACTIVATE,
            event = "APPLICATION_DEACTIVATED",
            resourceType = "APPLICATION")
    public ApplicationOutput inactivate(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application application = finder.findActive(identifier, workspaceId);
        List<String> manualTags = tags.findManual(application);
        application.inactivate(LocalDateTime.now());
        Application savedApplication = repository.saveAndFlush(application);
        return mapper.toOutput(savedApplication, workspaceIdentifier, manualTags);
    }

    @Transactional
    @Auditable(
            action = AuditAction.RESTORE,
            event = "APPLICATION_RESTORED",
            resourceType = "APPLICATION")
    public ApplicationOutput restore(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application application = finder.findInactiveForRestore(identifier, workspaceId);
        List<String> manualTags = tags.findManual(application);
        application.restore(LocalDateTime.now());
        Application savedApplication = repository.saveAndFlush(application);
        return mapper.toOutput(savedApplication, workspaceIdentifier, manualTags);
    }

    @Transactional
    @Auditable(
            action = AuditAction.DELETE,
            event = "APPLICATION_DELETED",
            resourceType = "APPLICATION")
    public ApplicationOutput delete(String workspaceIdentifier, String identifier) {
        Long workspaceId = workspaces.resolveInternalId(workspaceIdentifier);
        Application application = finder.findInactiveForDeletion(identifier, workspaceId);
        application.quarantine(LocalDateTime.now());
        Application savedApplication = repository.saveAndFlush(application);
        return mapper.toOutput(
                savedApplication, workspaceIdentifier, tags.findManual(savedApplication));
    }
}
