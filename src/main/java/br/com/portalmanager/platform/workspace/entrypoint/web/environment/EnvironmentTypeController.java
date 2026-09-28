package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationAccessPolicy;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentCompatibilityOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeInput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.model.EnvironmentTypeOutput;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.EnvironmentTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/environment-types")
@AuthorizationAccessPolicy(read = AuthorizationLevel.OPEN, write = AuthorizationLevel.OWNER)
public class EnvironmentTypeController {
    private final EnvironmentTypeService service;
    public EnvironmentTypeController(EnvironmentTypeService service) { this.service = service; }

    @PostMapping
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<EnvironmentTypeOutput> create(@RequestBody EnvironmentTypeInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(input));
    }
    @GetMapping
    public List<EnvironmentTypeOutput> list() { return service.list(); }
    @GetMapping("/{identifier}")
    public EnvironmentTypeOutput find(@PathVariable String identifier) { return service.findOutput(identifier); }
    @PutMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public EnvironmentTypeOutput update(@PathVariable String identifier, @RequestBody EnvironmentTypeInput input) {
        return service.update(identifier, input);
    }
    @PostMapping("/{identifier}/inactivate")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<Void> inactivate(@PathVariable String identifier) {
        service.inactivate(identifier);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{identifier}/restore")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public EnvironmentTypeOutput restore(@PathVariable String identifier) { return service.restore(identifier); }
    @DeleteMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        service.delete(identifier);
        return ResponseEntity.noContent().build();
    }
    @GetMapping("/compatibilities")
    public List<EnvironmentCompatibilityOutput> compatibilities() { return service.listCompatibilities(); }
    @PostMapping("/compatibilities")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<EnvironmentCompatibilityOutput> allow(@RequestBody CompatibilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.allow(request.parentTypeCode(), request.childTypeCode()));
    }
    @PostMapping("/compatibilities/{identifier}/inactivate")
    @AuthorizationRequired(level = AuthorizationLevel.OWNER)
    public ResponseEntity<Void> disallow(@PathVariable String identifier) {
        service.disallow(identifier);
        return ResponseEntity.noContent().build();
    }
    public record CompatibilityRequest(String parentTypeCode, String childTypeCode) {}
}
