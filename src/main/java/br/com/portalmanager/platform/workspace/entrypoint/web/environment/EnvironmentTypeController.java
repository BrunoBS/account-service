package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationAccessPolicy;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentCompatibilityOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environmenttype.EnvironmentTypeQueryService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility.EnvironmentTypeCompatibilityCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.compatibility.EnvironmentTypeCompatibilityQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/environment-types")
@AuthorizationAccessPolicy(read = AuthorizationLevel.OPEN, write = AuthorizationLevel.OWNER)
public class EnvironmentTypeController {
    private final EnvironmentTypeCommandService command;
    private final EnvironmentTypeQueryService query;
    private final EnvironmentTypeCompatibilityCommandService compatibilityCommand;
    private final EnvironmentTypeCompatibilityQueryService compatibilityQuery;
    public EnvironmentTypeController(EnvironmentTypeCommandService command, EnvironmentTypeQueryService query,
                                     EnvironmentTypeCompatibilityCommandService compatibilityCommand,
                                     EnvironmentTypeCompatibilityQueryService compatibilityQuery) {
        this.command = command;
        this.query = query;
        this.compatibilityCommand = compatibilityCommand;
        this.compatibilityQuery = compatibilityQuery;
    }

    @PostMapping
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<EnvironmentTypeOutput> create(@RequestBody EnvironmentTypeInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(command.create(input));
    }
    @GetMapping
    public List<EnvironmentTypeOutput> list() { return query.list(); }
    @GetMapping("/{identifier}")
    public EnvironmentTypeOutput find(@PathVariable String identifier) { return query.findOutput(identifier); }
    @PutMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public EnvironmentTypeOutput update(@PathVariable String identifier, @RequestBody EnvironmentTypeInput input) {
        return command.update(identifier, input);
    }
    @PostMapping("/{identifier}/inactivate")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<Void> inactivate(@PathVariable String identifier) {
        command.inactivate(identifier);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{identifier}/restore")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public EnvironmentTypeOutput restore(@PathVariable String identifier) { return command.restore(identifier); }
    @DeleteMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        command.delete(identifier);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/compatibilities")
    public List<EnvironmentCompatibilityOutput> compatibilities() { return compatibilityQuery.list(); }
    @PostMapping("/compatibilities")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<EnvironmentCompatibilityOutput> allow(@RequestBody CompatibilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(compatibilityCommand.allow(request.parentTypeCode(), request.childTypeCode()));
    }
    @PostMapping("/compatibilities/{identifier}/inactivate")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<Void> disallow(@PathVariable String identifier) {
        compatibilityCommand.disallow(identifier);
        return ResponseEntity.noContent().build();
    }
    public record CompatibilityRequest(String parentTypeCode, String childTypeCode) {}
}
