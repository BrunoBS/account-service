package br.com.portalmanager.platform.workspace.entrypoint.web.workspace;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.core.workspace.facade.WorkspaceFacade;
import br.com.portalmanager.platform.workspace.entrypoint.web.workspace.request.*;
import br.com.portalmanager.platform.workspace.entrypoint.web.workspace.response.WorkspaceResponse;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceFacade facade;

    public WorkspaceController(WorkspaceFacade f) {
        facade = f;
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(
        AuthorizationContext context,
        @RequestBody CreateWorkspaceRequest r
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            WorkspaceResponse.from(facade.create(context, r.toInput()))
        );
    }

    @GetMapping("/{identifier}")
    public WorkspaceResponse findByIdentifier(AuthorizationContext context, @PathVariable String identifier) {
        return WorkspaceResponse.from(facade.findByIdentifier(context, identifier));
    }

    @GetMapping
    public List<WorkspaceResponse> findAll(
        AuthorizationContext context,
        @RequestParam(defaultValue = "true") Boolean active,
        @RequestParam(required = false) String typeName,
        @RequestParam(required = false) String tagName
    ) {
        return facade.findAll(context, active, typeName, tagName).stream().map(WorkspaceResponse::from).toList();
    }

    @PutMapping("/{identifier}")
    public WorkspaceResponse update(
        AuthorizationContext context,
        @PathVariable String identifier,
        @RequestBody UpdateWorkspaceRequest r
    ) {
        return WorkspaceResponse.from(facade.update(context, identifier, r.toInput()));
    }

    @PostMapping("/{identifier}/inactivate")
    public ResponseEntity<Void> inactivate(AuthorizationContext context, @PathVariable String identifier) {
        facade.inactivate(context, identifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/restore")
    public WorkspaceResponse restore(AuthorizationContext context, @PathVariable String identifier) {
        return WorkspaceResponse.from(facade.restore(context, identifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(AuthorizationContext context, @PathVariable String identifier) {
        facade.delete(context, identifier);
        return ResponseEntity.noContent().build();
    }
}
