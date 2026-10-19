package br.com.portalmanager.platform.workspace.core.environment.facade.environmenttype;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environmenttype.EnvironmentTypeInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.environmenttype.EnvironmentTypeOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentTypeFacade {

    private final EnvironmentTypeCommandService command;
    private final EnvironmentTypeQueryService query;

    public EnvironmentTypeFacade(EnvironmentTypeCommandService command, EnvironmentTypeQueryService query) {
        this.command = command;
        this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public EnvironmentTypeOutput create(AuthorizationContext context, EnvironmentTypeInput input) {
        return command.create(input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public List<EnvironmentTypeOutput> list(AuthorizationContext context, String lifecycle) {
        return query.list(lifecycle);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public EnvironmentTypeOutput find(AuthorizationContext context, String code) {
        return query.find(code);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public EnvironmentTypeOutput update(AuthorizationContext context, String code, EnvironmentTypeInput input) {
        return command.update(code, input);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(AuthorizationContext context, String code) {
        command.inactivate(code);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.RESTORE)
    public EnvironmentTypeOutput restore(AuthorizationContext context, String code) {
        return command.restore(code);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String code) {
        command.delete(code);
    }
}
