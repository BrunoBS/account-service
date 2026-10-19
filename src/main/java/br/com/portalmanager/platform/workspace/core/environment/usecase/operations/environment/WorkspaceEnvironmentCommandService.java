package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.UpdateEnvironmentInput;
import br.com.portalmanager.platform.workspace.foundation.integration.WorkspaceReferencePort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceEnvironmentCommandService {

    private final EnvironmentCommandOperations operations;
    private final WorkspaceReferencePort workspaces;

    public WorkspaceEnvironmentCommandService(
        EnvironmentCommandOperations operations,
        WorkspaceReferencePort workspaces
    ) {
        this.operations = operations;
        this.workspaces = workspaces;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "ENVIRONMENT_CREATED", resourceType = "ENVIRONMENT")
    public EnvironmentOutput create(String workspaceIdentifier, CreateEnvironmentInput input) {
        Long workspaceId = workspaces.resolveActiveForUpdate(workspaceIdentifier).id();
        return operations.create(workspaceId, workspaceIdentifier, input);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "ENVIRONMENT_UPDATED", resourceType = "ENVIRONMENT")
    public EnvironmentOutput update(String workspaceIdentifier, String identifier, UpdateEnvironmentInput rawInput) {
        return operations.update(workspaces.resolveInternalId(workspaceIdentifier), workspaceIdentifier, identifier, rawInput);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "ENVIRONMENT_DEACTIVATED", resourceType = "ENVIRONMENT")
    public EnvironmentOutput inactivate(String workspaceIdentifier, String identifier) {
        return operations.inactivate(workspaces.resolveInternalId(workspaceIdentifier), workspaceIdentifier, identifier);
    }

    @Transactional
    @Auditable(action = AuditAction.RESTORE, event = "ENVIRONMENT_RESTORED", resourceType = "ENVIRONMENT")
    public EnvironmentOutput restore(String workspaceIdentifier, String identifier) {
        return operations.restore(workspaces.resolveInternalId(workspaceIdentifier), workspaceIdentifier, identifier);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "ENVIRONMENT_DELETED", resourceType = "ENVIRONMENT")
    public EnvironmentOutput delete(String workspaceIdentifier, String identifier) {
        return operations.delete(workspaces.resolveInternalId(workspaceIdentifier), workspaceIdentifier, identifier);
    }
}
