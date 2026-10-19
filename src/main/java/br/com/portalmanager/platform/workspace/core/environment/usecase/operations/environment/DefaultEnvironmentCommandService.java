package br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment;

import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.audit.model.AuditAction;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environment.UpdateEnvironmentInput;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DefaultEnvironmentCommandService {

    private final EnvironmentCommandOperations operations;

    public DefaultEnvironmentCommandService(EnvironmentCommandOperations operations) {
        this.operations = operations;
    }

    @Transactional
    @Auditable(action = AuditAction.CREATE, event = "DEFAULT_ENVIRONMENT_CREATED", resourceType = "ENVIRONMENT")
    public EnvironmentOutput create(CreateEnvironmentInput input) {
        return operations.create(null, null, input);
    }

    @Transactional
    @Auditable(action = AuditAction.UPDATE, event = "DEFAULT_ENVIRONMENT_UPDATED", resourceType = "ENVIRONMENT")
    public EnvironmentOutput update(String identifier, UpdateEnvironmentInput rawInput) {
        return operations.update(null, null, identifier, rawInput);
    }

    @Transactional
    @Auditable(action = AuditAction.DEACTIVATE, event = "DEFAULT_ENVIRONMENT_DEACTIVATED", resourceType = "ENVIRONMENT")
    public EnvironmentOutput inactivate(String identifier) {
        return operations.inactivate(null, null, identifier);
    }

    @Transactional
    @Auditable(action = AuditAction.RESTORE, event = "DEFAULT_ENVIRONMENT_RESTORED", resourceType = "ENVIRONMENT")
    public EnvironmentOutput restore(String identifier) {
        return operations.restore(null, null, identifier);
    }

    @Transactional
    @Auditable(action = AuditAction.DELETE, event = "DEFAULT_ENVIRONMENT_DELETED", resourceType = "ENVIRONMENT")
    public EnvironmentOutput delete(String identifier) {
        return operations.delete(null, null, identifier);
    }
}
