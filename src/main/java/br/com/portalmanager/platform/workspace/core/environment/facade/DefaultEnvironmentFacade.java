package br.com.portalmanager.platform.workspace.core.environment.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.*;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.*;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.*;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DefaultEnvironmentFacade {

    private final EnvironmentCommandService command;
    private final EnvironmentQueryService query;

    public DefaultEnvironmentFacade(EnvironmentCommandService c, EnvironmentQueryService q) {
        command = c;
        query = q;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public EnvironmentOutput create(AuthorizationContext context, CreateEnvironmentInput i) {
        return command.createDefault(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public List<EnvironmentOutput> list(AuthorizationContext context, Boolean a) {
        return query.listDefaults(a);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public EnvironmentOutput find(AuthorizationContext context, String i) {
        return query.findDefault(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public EnvironmentOutput update(AuthorizationContext context, String i, UpdateEnvironmentInput in) {
        return command.updateDefault(i, in);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(AuthorizationContext context, String i) {
        command.inactivateDefault(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.RESTORE)
    public EnvironmentOutput restore(AuthorizationContext context, String i) {
        return command.restoreDefault(i);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String i) {
        command.deleteDefault(i);
    }
}
