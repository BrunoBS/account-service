package br.com.portalmanager.platform.workspace.entrypoint.workspace;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.core.workspace.facade.WorkspaceFacade;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.request.CreateWorkspaceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.request.UpdateWorkspaceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.response.WorkspaceResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceFacade facade;

    public WorkspaceController(WorkspaceFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(
        AuthorizationContext context,
        @RequestBody CreateWorkspaceRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            WorkspaceResponse.from(facade.create(context, request.toInput()))
        );
    }

    @GetMapping("/{identifier}")
    public WorkspaceResponse findByIdentifier(AuthorizationContext context, @PathVariable String identifier) {
        return WorkspaceResponse.from(facade.findByIdentifier(context, identifier));
    }

    @GetMapping
    public PagedModel<WorkspaceResponse> findAll(
        AuthorizationContext context,
        @RequestParam(required = false) String lifecycle,
        @RequestParam(required = false) String typeName,
        @RequestParam(required = false) String tagName,
        @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return new PagedModel<>(
            facade.findAll(context, lifecycle, typeName, tagName, pageable).map(WorkspaceResponse::from)
        );
    }

    @PutMapping("/{identifier}")
    public WorkspaceResponse update(
        AuthorizationContext context,
        @PathVariable String identifier,
        @RequestBody UpdateWorkspaceRequest request
    ) {
        return WorkspaceResponse.from(facade.update(context, identifier, request.toInput()));
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
