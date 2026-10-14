package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.core.environment.facade.WorkspaceEnvironmentFacade;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.CreateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.UpdateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentTreeResponse;
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
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/environments")
public class WorkspaceEnvironmentController {

    private final WorkspaceEnvironmentFacade facade;

    public WorkspaceEnvironmentController(WorkspaceEnvironmentFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<EnvironmentResponse> create(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @RequestBody CreateEnvironmentRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            EnvironmentResponse.from(facade.create(context, workspaceIdentifier, request.toInput()))
        );
    }

    @GetMapping
    public List<EnvironmentResponse> list(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @RequestParam(required = false) String lifecycle
    ) {
        return facade.list(context, workspaceIdentifier, lifecycle).stream().map(EnvironmentResponse::from).toList();
    }

    @GetMapping("/{identifier}")
    public EnvironmentResponse find(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier
    ) {
        return EnvironmentResponse.from(facade.find(context, workspaceIdentifier, identifier));
    }

    @GetMapping("/roots")
    public List<EnvironmentResponse> findRoots(AuthorizationContext context, @PathVariable String workspaceIdentifier) {
        return facade.findRoots(context, workspaceIdentifier).stream().map(EnvironmentResponse::from).toList();
    }

    @GetMapping("/{identifier}/children")
    public List<EnvironmentResponse> findChildren(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier
    ) {
        return facade
            .findChildren(context, workspaceIdentifier, identifier)
            .stream()
            .map(EnvironmentResponse::from)
            .toList();
    }

    @GetMapping("/tree")
    public List<EnvironmentTreeResponse> findTree(AuthorizationContext context, @PathVariable String workspaceIdentifier) {
        return facade.findTree(context, workspaceIdentifier).stream().map(EnvironmentTreeResponse::from).toList();
    }

    @PutMapping("/{identifier}")
    public EnvironmentResponse update(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier,
        @RequestBody UpdateEnvironmentRequest request
    ) {
        return EnvironmentResponse.from(facade.update(context, workspaceIdentifier, identifier, request.toInput()));
    }

    @PostMapping("/{identifier}/inactivate")
    public ResponseEntity<Void> inactivate(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier
    ) {
        facade.inactivate(context, workspaceIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/restore")
    public EnvironmentResponse restore(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier
    ) {
        return EnvironmentResponse.from(facade.restore(context, workspaceIdentifier, identifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier
    ) {
        facade.delete(context, workspaceIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }
}
