package br.com.portalmanager.platform.workspace.core.environment.facade;

import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility.EnvironmentTypeCompatibilityCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility.EnvironmentTypeCompatibilityQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.CreateEnvironmentTypeCompatibilityRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentTypeCompatibilityResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationAction;
import org.springframework.stereotype.Service;

@Service
public class EnvironmentTypeCompatibilityFacade {
    private final EnvironmentTypeCompatibilityCommandService command;
    private final EnvironmentTypeCompatibilityQueryService query;

    public EnvironmentTypeCompatibilityFacade(EnvironmentTypeCompatibilityCommandService command,
                                                   EnvironmentTypeCompatibilityQueryService query) {
        this.command = command;
        this.query = query;
    }

    @AuthorizationRequired(level = AuthorizationLevel.OPEN, action = AuthorizationAction.READ)
    public List<EnvironmentTypeCompatibilityResponse> list(String lifecycle) {
        return query.list(lifecycle).stream()
                .map(EnvironmentTypeCompatibilityResponse::from)
                .toList();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.CREATE)
    public ResponseEntity<EnvironmentTypeCompatibilityResponse> allow(CreateEnvironmentTypeCompatibilityRequest request) {
        String parent = request == null ? null : request.parentTypeCode();
        String child = request == null ? null : request.childTypeCode();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EnvironmentTypeCompatibilityResponse.from(command.allow(parent, child)));
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DEACTIVATE)
    public ResponseEntity<Void> disallow(String identifier) {
        command.disallow(identifier);
        return ResponseEntity.noContent().build();
    }

    @AuthorizationRequired(level = AuthorizationLevel.OWNER, action = AuthorizationAction.DELETE)
    public ResponseEntity<Void> delete(String identifier) {
        command.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
