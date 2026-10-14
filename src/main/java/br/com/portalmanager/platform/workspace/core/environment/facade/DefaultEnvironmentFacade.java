package br.com.portalmanager.platform.workspace.core.environment.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.CreateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.UpdateEnvironmentInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.DefaultEnvironmentCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultEnvironmentFacade {

    private final DefaultEnvironmentCommandService command;
    private final EnvironmentQueryService query;

    public DefaultEnvironmentFacade(DefaultEnvironmentCommandService command, EnvironmentQueryService query) {
        this.command = command;
        this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public EnvironmentOutput create(AuthorizationContext context, CreateEnvironmentInput input) {
        return command.create(input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public List<EnvironmentOutput> list(AuthorizationContext context, String lifecycle) {
        return query.listDefaults(lifecycle);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public EnvironmentOutput find(AuthorizationContext context, String identifier) {
        return query.findDefault(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public EnvironmentOutput update(AuthorizationContext context, String identifier, UpdateEnvironmentInput input) {
        return command.update(identifier, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(AuthorizationContext context, String identifier) {
        command.inactivate(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.RESTORE)
    public EnvironmentOutput restore(AuthorizationContext context, String identifier) {
        return command.restore(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String identifier) {
        command.delete(identifier);
    }
}
