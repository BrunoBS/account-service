package br.com.portalmanager.platform.workspace.core.environment.facade;

import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.CreateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.UpdateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import org.springframework.stereotype.Service;

@Service
public class DefaultEnvironmentFacade {
    private final EnvironmentCommandService command;
    private final EnvironmentQueryService query;
    public DefaultEnvironmentFacade(EnvironmentCommandService command, EnvironmentQueryService query) {
        this.command = command;
        this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public ResponseEntity<EnvironmentResponse> create(CreateEnvironmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(EnvironmentResponse.from(command.createDefault(request.toInput())));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public List<EnvironmentResponse> list(Boolean active) {
        return query.listDefaults(active).stream().map(EnvironmentResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public EnvironmentResponse find(String identifier) {
        return EnvironmentResponse.from(query.findDefault(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public EnvironmentResponse update(String identifier, UpdateEnvironmentRequest request) {
        return EnvironmentResponse.from(command.updateDefault(identifier, request.toInput()));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public ResponseEntity<Void> inactivate(String identifier) {
        command.inactivateDefault(identifier);
        return ResponseEntity.noContent().build();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.RESTORE)
    public EnvironmentResponse restore(String identifier) {
        return EnvironmentResponse.from(command.restoreDefault(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public ResponseEntity<Void> delete(String identifier) {
        command.deleteDefault(identifier);
        return ResponseEntity.noContent().build();
    }
}
