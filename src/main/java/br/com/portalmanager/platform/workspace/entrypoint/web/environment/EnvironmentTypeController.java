package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.CreateEnvironmentTypeRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.UpdateEnvironmentTypeRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentTypeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/environment-types")
@AuthorizationRequired(level = AuthorizationLevel.OPEN)
public class EnvironmentTypeController {
    private final EnvironmentTypeCommandService command;
    private final EnvironmentTypeQueryService query;
    public EnvironmentTypeController(EnvironmentTypeCommandService command, EnvironmentTypeQueryService query) {
        this.command = command;
        this.query = query;
    }

    @PostMapping
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<EnvironmentTypeResponse> create(@RequestBody CreateEnvironmentTypeRequest request) {
        var input = request == null ? null : request.toInput();
        return ResponseEntity.status(HttpStatus.CREATED).body(EnvironmentTypeResponse.from(command.create(input)));
    }
    @GetMapping
    public List<EnvironmentTypeResponse> list() {
        return query.list().stream().map(EnvironmentTypeResponse::from).toList();
    }
    @GetMapping("/{identifier}")
    public EnvironmentTypeResponse find(@PathVariable String identifier) {
        return EnvironmentTypeResponse.from(query.findOutput(identifier));
    }
    @PutMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public EnvironmentTypeResponse update(@PathVariable String identifier, @RequestBody UpdateEnvironmentTypeRequest request) {
        var input = request == null ? null : request.toInput();
        return EnvironmentTypeResponse.from(command.update(identifier, input));
    }
    @PostMapping("/{identifier}/inactivate")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<Void> inactivate(@PathVariable String identifier) {
        command.inactivate(identifier);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{identifier}/restore")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public EnvironmentTypeResponse restore(@PathVariable String identifier) {
        return EnvironmentTypeResponse.from(command.restore(identifier));
    }
    @DeleteMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        command.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
