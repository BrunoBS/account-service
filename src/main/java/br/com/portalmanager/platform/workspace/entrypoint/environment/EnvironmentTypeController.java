package br.com.portalmanager.platform.workspace.entrypoint.environment;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.core.environment.facade.environmenttype.EnvironmentTypeFacade;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.CreateEnvironmentTypeRequest;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.UpdateEnvironmentTypeRequest;
import br.com.portalmanager.platform.workspace.entrypoint.environment.response.EnvironmentTypeResponse;
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
@RequestMapping("/api/v1/environment-types")
public class EnvironmentTypeController {

    private final EnvironmentTypeFacade facade;

    public EnvironmentTypeController(EnvironmentTypeFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<EnvironmentTypeResponse> create(
        AuthorizationContext context,
        @RequestBody CreateEnvironmentTypeRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            EnvironmentTypeResponse.from(facade.create(context, request.toInput()))
        );
    }

    @GetMapping
    public List<EnvironmentTypeResponse> list(
        AuthorizationContext context,
        @RequestParam(required = false) String lifecycle
    ) {
        return facade.list(context, lifecycle).stream().map(EnvironmentTypeResponse::from).toList();
    }

    @GetMapping("/{code}")
    public EnvironmentTypeResponse find(AuthorizationContext context, @PathVariable String code) {
        return EnvironmentTypeResponse.from(facade.find(context, code));
    }

    @PutMapping("/{code}")
    public EnvironmentTypeResponse update(
        AuthorizationContext context,
        @PathVariable String code,
        @RequestBody UpdateEnvironmentTypeRequest request
    ) {
        return EnvironmentTypeResponse.from(facade.update(context, code, request.toInput()));
    }

    @PostMapping("/{code}/inactivate")
    public ResponseEntity<Void> inactivate(AuthorizationContext context, @PathVariable String code) {
        facade.inactivate(context, code);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{code}/restore")
    public EnvironmentTypeResponse restore(AuthorizationContext context, @PathVariable String code) {
        return EnvironmentTypeResponse.from(facade.restore(context, code));
    }

    @DeleteMapping("/{code}")
    public ResponseEntity<Void> delete(AuthorizationContext context, @PathVariable String code) {
        facade.delete(context, code);
        return ResponseEntity.noContent().build();
    }
}
