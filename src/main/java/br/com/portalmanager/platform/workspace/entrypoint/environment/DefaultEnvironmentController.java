package br.com.portalmanager.platform.workspace.entrypoint.environment;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.core.environment.facade.environment.DefaultEnvironmentFacade;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.CreateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.UpdateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.environment.response.EnvironmentResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/environment-defaults")
public class DefaultEnvironmentController {

    private final DefaultEnvironmentFacade facade;

    public DefaultEnvironmentController(DefaultEnvironmentFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<EnvironmentResponse> create(
        AuthorizationContext context,
        @RequestBody CreateEnvironmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            EnvironmentResponse.from(facade.create(context, request.toInput()))
        );
    }

    @GetMapping
    public List<EnvironmentResponse> list(
        AuthorizationContext context,
        @RequestParam(required = false) String lifecycle
    ) {
        return facade.list(context, lifecycle).stream().map(EnvironmentResponse::from).toList();
    }

    @GetMapping("/{identifier}")
    public EnvironmentResponse find(AuthorizationContext context, @PathVariable String identifier) {
        return EnvironmentResponse.from(facade.find(context, identifier));
    }

    @PutMapping("/{identifier}")
    public EnvironmentResponse update(
        AuthorizationContext context,
        @PathVariable String identifier,
        @RequestBody UpdateEnvironmentRequest request
    ) {
        return EnvironmentResponse.from(facade.update(context, identifier, request.toInput()));
    }

    @PostMapping("/{identifier}/inactivate")
    public ResponseEntity<Void> inactivate(AuthorizationContext context, @PathVariable String identifier) {
        facade.inactivate(context, identifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/restore")
    public EnvironmentResponse restore(AuthorizationContext context, @PathVariable String identifier) {
        return EnvironmentResponse.from(facade.restore(context, identifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(AuthorizationContext context, @PathVariable String identifier) {
        facade.delete(context, identifier);
        return ResponseEntity.noContent().build();
    }
}
