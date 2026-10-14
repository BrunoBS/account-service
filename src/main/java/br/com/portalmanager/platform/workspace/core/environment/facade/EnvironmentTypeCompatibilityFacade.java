package br.com.portalmanager.platform.workspace.core.environment.facade;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentCompatibilityOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility.EnvironmentTypeCompatibilityCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility.EnvironmentTypeCompatibilityQueryService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentTypeCompatibilityFacade {

    private final EnvironmentTypeCompatibilityCommandService command;
    private final EnvironmentTypeCompatibilityQueryService query;

    public EnvironmentTypeCompatibilityFacade(
        EnvironmentTypeCompatibilityCommandService command,
        EnvironmentTypeCompatibilityQueryService query
    ) {
        this.command = command;
        this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public List<EnvironmentCompatibilityOutput> list(AuthorizationContext context, String lifecycle) {
        return query.list(lifecycle);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public EnvironmentCompatibilityOutput find(AuthorizationContext context, String identifier) {
        return query.find(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public EnvironmentCompatibilityOutput create(
        AuthorizationContext context,
        String parentTypeCode,
        String childTypeCode
    ) {
        return command.create(parentTypeCode, childTypeCode);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public void inactivate(AuthorizationContext context, String identifier) {
        command.inactivate(identifier);
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public void delete(AuthorizationContext context, String identifier) {
        command.delete(identifier);
    }
}
