package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.workspace.core.environment.facade.DefaultEnvironmentFacade;

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
public class DefaultEnvironmentController {
    private final DefaultEnvironmentFacade facade;

    public DefaultEnvironmentController(DefaultEnvironmentFacade facade) { this.facade = facade; }

    @PostMapping

    public ResponseEntity<EnvironmentResponse> create(@RequestBody CreateEnvironmentRequest request) {
        return facade.create(request);
    }

    @GetMapping

    public List<EnvironmentResponse> list(@RequestParam(defaultValue = "true") Boolean active) {
        return facade.list(active);
    }

    @GetMapping("/{identifier}")

    public EnvironmentResponse find(@PathVariable String identifier) {
        return facade.find(identifier);
    }

    @PutMapping("/{identifier}")

    public EnvironmentResponse update(@PathVariable String identifier, @RequestBody UpdateEnvironmentRequest request) {
        return facade.update(identifier, request);
    }

    @PostMapping("/{identifier}/inactivate")

    public ResponseEntity<Void> inactivate(@PathVariable String identifier) {
        return facade.inactivate(identifier);
    }

    @PostMapping("/{identifier}/restore")

    public EnvironmentResponse restore(@PathVariable String identifier) {
        return facade.restore(identifier);
    }

    @DeleteMapping("/{identifier}")

    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        return facade.delete(identifier);
    }
}
