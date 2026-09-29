package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationAccessPolicy;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.CreateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.UpdateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/environment-defaults")
@AuthorizationAccessPolicy(read = AuthorizationLevel.OPEN, write = AuthorizationLevel.OWNER)
public class DefaultEnvironmentController {
    private final EnvironmentCommandService command;
    private final EnvironmentQueryService query;
    public DefaultEnvironmentController(EnvironmentCommandService command, EnvironmentQueryService query) {
        this.command = command;
        this.query = query;
    }

    @PostMapping
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<EnvironmentResponse> create(@RequestBody CreateEnvironmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(EnvironmentResponse.from(command.createDefault(request.toInput())));
    }

    @GetMapping
    public List<EnvironmentResponse> list(@RequestParam(defaultValue = "true") Boolean active) {
        return query.listDefaults(active).stream().map(EnvironmentResponse::from).toList();
    }

    @GetMapping("/{identifier}")
    public EnvironmentResponse find(@PathVariable String identifier) {
        return EnvironmentResponse.from(query.findDefault(identifier));
    }

    @PutMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public EnvironmentResponse update(@PathVariable String identifier, @RequestBody UpdateEnvironmentRequest request) {
        return EnvironmentResponse.from(command.updateDefault(identifier, request.toInput()));
    }

    @PostMapping("/{identifier}/inactivate")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<Void> inactivate(@PathVariable String identifier) {
        command.inactivateDefault(identifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/restore")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public EnvironmentResponse restore(@PathVariable String identifier) {
        return EnvironmentResponse.from(command.restoreDefault(identifier));
    }

    @DeleteMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        command.deleteDefault(identifier);
        return ResponseEntity.noContent().build();
    }
}
