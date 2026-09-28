package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationAccessPolicy;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentCompatibilityOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility.EnvironmentTypeCompatibilityCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility.EnvironmentTypeCompatibilityQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/environment-types/compatibilities")
@AuthorizationAccessPolicy(read = AuthorizationLevel.OPEN, write = AuthorizationLevel.OWNER)
public class EnvironmentTypeCompatibilityController {
    private final EnvironmentTypeCompatibilityCommandService command;
    private final EnvironmentTypeCompatibilityQueryService query;

    public EnvironmentTypeCompatibilityController(EnvironmentTypeCompatibilityCommandService command,
                                                  EnvironmentTypeCompatibilityQueryService query) {
        this.command = command;
        this.query = query;
    }

    @GetMapping
    public List<EnvironmentCompatibilityOutput> list() { return query.list(); }

    @PostMapping
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<EnvironmentCompatibilityOutput> allow(@RequestBody CompatibilityRequest request) {
        String parent = request == null ? null : request.parentTypeCode();
        String child = request == null ? null : request.childTypeCode();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(command.allow(parent, child));
    }

    @PostMapping("/{identifier}/inactivate")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<Void> disallow(@PathVariable String identifier) {
        command.disallow(identifier);
        return ResponseEntity.noContent().build();
    }

    public record CompatibilityRequest(String parentTypeCode, String childTypeCode) {}
}
