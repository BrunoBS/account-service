package br.com.portalmanager.platform.workspace.core.environment.facade;

import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.CreateEnvironmentTypeRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.UpdateEnvironmentTypeRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentTypeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
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
    public ResponseEntity<EnvironmentTypeResponse> create(CreateEnvironmentTypeRequest request) {
        var input = request == null ? null : request.toInput();
        return ResponseEntity.status(HttpStatus.CREATED).body(EnvironmentTypeResponse.from(command.create(input)));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public List<EnvironmentTypeResponse> list() {
        return query.list().stream().map(EnvironmentTypeResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public EnvironmentTypeResponse find(String identifier) {
        return EnvironmentTypeResponse.from(query.findOutput(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.UPDATE)
    public EnvironmentTypeResponse update(String identifier, UpdateEnvironmentTypeRequest request) {
        var input = request == null ? null : request.toInput();
        return EnvironmentTypeResponse.from(command.update(identifier, input));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public ResponseEntity<Void> inactivate(String identifier) {
        command.inactivate(identifier);
        return ResponseEntity.noContent().build();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.RESTORE)
    public EnvironmentTypeResponse restore(String identifier) {
        return EnvironmentTypeResponse.from(command.restore(identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public ResponseEntity<Void> delete(String identifier) {
        command.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
