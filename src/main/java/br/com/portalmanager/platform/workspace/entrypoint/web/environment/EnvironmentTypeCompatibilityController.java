package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility.EnvironmentTypeCompatibilityCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility.EnvironmentTypeCompatibilityQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.CreateEnvironmentTypeCompatibilityRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentTypeCompatibilityResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/environment-types/compatibilities")
@AuthorizationRequired(level = AuthorizationLevel.OPEN)
public class EnvironmentTypeCompatibilityController {
    private final EnvironmentTypeCompatibilityCommandService command;
    private final EnvironmentTypeCompatibilityQueryService query;

    public EnvironmentTypeCompatibilityController(EnvironmentTypeCompatibilityCommandService command,
                                                   EnvironmentTypeCompatibilityQueryService query) {
        this.command = command;
        this.query = query;
    }

    @GetMapping
    public List<EnvironmentTypeCompatibilityResponse> list(
            @RequestParam(required = false) String lifecycle) {
        return query.list(lifecycle).stream()
                .map(EnvironmentTypeCompatibilityResponse::from)
                .toList();
    }

    @PostMapping
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<EnvironmentTypeCompatibilityResponse> allow(@RequestBody CreateEnvironmentTypeCompatibilityRequest request) {
        String parent = request == null ? null : request.parentTypeCode();
        String child = request == null ? null : request.childTypeCode();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EnvironmentTypeCompatibilityResponse.from(command.allow(parent, child)));
    }

    @PostMapping("/{identifier}/inactivate")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<Void> disallow(@PathVariable String identifier) {
        command.disallow(identifier);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        command.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
