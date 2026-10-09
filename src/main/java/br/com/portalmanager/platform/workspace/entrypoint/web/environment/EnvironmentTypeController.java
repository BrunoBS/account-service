package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.workspace.core.environment.facade.EnvironmentTypeFacade;

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
public class EnvironmentTypeController {
    private final EnvironmentTypeFacade facade;

    public EnvironmentTypeController(EnvironmentTypeFacade facade) { this.facade = facade; }

    @PostMapping

    public ResponseEntity<EnvironmentTypeResponse> create(@RequestBody CreateEnvironmentTypeRequest request) {
        return facade.create(request);
    }
    @GetMapping

    public List<EnvironmentTypeResponse> list() {
        return facade.list();
    }
    @GetMapping("/{identifier}")

    public EnvironmentTypeResponse find(@PathVariable String identifier) {
        return facade.find(identifier);
    }
    @PutMapping("/{identifier}")

    public EnvironmentTypeResponse update(@PathVariable String identifier, @RequestBody UpdateEnvironmentTypeRequest request) {
        return facade.update(identifier, request);
    }
    @PostMapping("/{identifier}/inactivate")

    public ResponseEntity<Void> inactivate(@PathVariable String identifier) {
        return facade.inactivate(identifier);
    }
    @PostMapping("/{identifier}/restore")

    public EnvironmentTypeResponse restore(@PathVariable String identifier) {
        return facade.restore(identifier);
    }
    @DeleteMapping("/{identifier}")

    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        return facade.delete(identifier);
    }
}
