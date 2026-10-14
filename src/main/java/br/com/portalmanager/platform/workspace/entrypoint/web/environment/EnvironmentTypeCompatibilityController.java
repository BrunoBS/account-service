package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.core.environment.facade.EnvironmentTypeCompatibilityFacade;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.CreateEnvironmentTypeCompatibilityRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentTypeCompatibilityResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/environment-types/compatibilities")
public class EnvironmentTypeCompatibilityController {

    private final EnvironmentTypeCompatibilityFacade facade;

    public EnvironmentTypeCompatibilityController(EnvironmentTypeCompatibilityFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public List<EnvironmentTypeCompatibilityResponse> list(
        AuthorizationContext context,
        @RequestParam(required = false) String lifecycle
    ) {
        return facade.list(context, lifecycle).stream().map(EnvironmentTypeCompatibilityResponse::from).toList();
    }

    @GetMapping("/{identifier}")
    public EnvironmentTypeCompatibilityResponse find(AuthorizationContext context, @PathVariable String identifier) {
        return EnvironmentTypeCompatibilityResponse.from(facade.find(context, identifier));
    }

    @PostMapping
    public ResponseEntity<EnvironmentTypeCompatibilityResponse> create(
        AuthorizationContext context,
        @RequestBody CreateEnvironmentTypeCompatibilityRequest request
    ) {
        String parentTypeCode = request == null ? null : request.parentTypeCode();
        String childTypeCode = request == null ? null : request.childTypeCode();
        return ResponseEntity.status(HttpStatus.CREATED).body(
            EnvironmentTypeCompatibilityResponse.from(facade.create(context, parentTypeCode, childTypeCode))
        );
    }

    @PostMapping("/{identifier}/inactivate")
    public ResponseEntity<Void> inactivate(AuthorizationContext context, @PathVariable String identifier) {
        facade.inactivate(context, identifier);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(AuthorizationContext context, @PathVariable String identifier) {
        facade.delete(context, identifier);
        return ResponseEntity.noContent().build();
    }
}
