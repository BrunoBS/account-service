package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.workspace.core.environment.facade.EnvironmentTypeCompatibilityFacade;

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
public class EnvironmentTypeCompatibilityController {
    private final EnvironmentTypeCompatibilityFacade facade;


    public EnvironmentTypeCompatibilityController(EnvironmentTypeCompatibilityFacade facade) { this.facade = facade; }

    @GetMapping

    public List<EnvironmentTypeCompatibilityResponse> list(
            @RequestParam(required = false) String lifecycle) {
        return facade.list(lifecycle);
    }

    @PostMapping

    public ResponseEntity<EnvironmentTypeCompatibilityResponse> allow(@RequestBody CreateEnvironmentTypeCompatibilityRequest request) {
        return facade.allow(request);
    }

    @PostMapping("/{identifier}/inactivate")

    public ResponseEntity<Void> disallow(@PathVariable String identifier) {
        return facade.disallow(identifier);
    }

    @DeleteMapping("/{identifier}")

    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        return facade.delete(identifier);
    }
}
