package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.core.environment.facade.EnvironmentTypeCompatibilityFacade;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.CreateEnvironmentTypeCompatibilityRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentTypeCompatibilityResponse;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/environment-types/compatibilities")
public class EnvironmentTypeCompatibilityController {

    private final EnvironmentTypeCompatibilityFacade facade;

    public EnvironmentTypeCompatibilityController(EnvironmentTypeCompatibilityFacade f) {
        facade = f;
    }

    @GetMapping
    public List<EnvironmentTypeCompatibilityResponse> list(
        AuthorizationContext context,
        @RequestParam(required = false) String lifecycle
    ) {
        return facade.list(context, lifecycle).stream().map(EnvironmentTypeCompatibilityResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<EnvironmentTypeCompatibilityResponse> allow(
        AuthorizationContext context,
        @RequestBody CreateEnvironmentTypeCompatibilityRequest r
    ) {
        String p = r == null ? null : r.parentTypeCode(),
            c = r == null ? null : r.childTypeCode();
        return ResponseEntity.status(HttpStatus.CREATED).body(
            EnvironmentTypeCompatibilityResponse.from(facade.allow(context, p, c))
        );
    }

    @PostMapping("/{identifier}/inactivate")
    public ResponseEntity<Void> disallow(AuthorizationContext context, @PathVariable String identifier) {
        facade.disallow(context, identifier);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(AuthorizationContext context, @PathVariable String identifier) {
        facade.delete(context, identifier);
        return ResponseEntity.noContent().build();
    }
}
