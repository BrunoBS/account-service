package br.com.portalmanager.platform.workspace.core.environment.facade;

import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.CreateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.UpdateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentTreeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceEnvironmentFacade {
    private final EnvironmentCommandService command;
    private final EnvironmentQueryService query;
    public WorkspaceEnvironmentFacade(EnvironmentCommandService command, EnvironmentQueryService query) {
        this.command = command;
        this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.CREATE)
    public ResponseEntity<EnvironmentResponse> create(String workspaceIdentifier, CreateEnvironmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(EnvironmentResponse.from(command.createCustom(workspaceIdentifier, request.toInput())));
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<EnvironmentResponse> list(String workspaceIdentifier, Boolean active) {
        return query.listCustom(workspaceIdentifier, active).stream().map(EnvironmentResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public EnvironmentResponse find(String workspaceIdentifier, String identifier) {
        return EnvironmentResponse.from(query.findCustom(workspaceIdentifier, identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<EnvironmentResponse> roots(String workspaceIdentifier) {
        return query.roots(workspaceIdentifier).stream().map(EnvironmentResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<EnvironmentResponse> children(String workspaceIdentifier, String identifier) {
        return query.children(workspaceIdentifier, identifier).stream().map(EnvironmentResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.DEV, action = AuthorizationAction.READ)
    public List<EnvironmentTreeResponse> tree(String workspaceIdentifier) {
        return query.tree(workspaceIdentifier).stream().map(EnvironmentTreeResponse::from).toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.UPDATE)
    public EnvironmentResponse update(String workspaceIdentifier, String identifier,
                                      UpdateEnvironmentRequest request) {
        return EnvironmentResponse.from(command.updateCustom(workspaceIdentifier, identifier, request.toInput()));
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DEACTIVATE)
    public ResponseEntity<Void> inactivate(String workspaceIdentifier, String identifier) {
        command.inactivateCustom(workspaceIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.RESTORE)
    public EnvironmentResponse restore(String workspaceIdentifier, String identifier) {
        return EnvironmentResponse.from(command.restoreCustom(workspaceIdentifier, identifier));
    }

    @AuthorizationRequired(level = AuthorizationLevel.ADM, action = AuthorizationAction.DELETE)
    public ResponseEntity<Void> delete(String workspaceIdentifier, String identifier) {
        command.deleteCustom(workspaceIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }
}
