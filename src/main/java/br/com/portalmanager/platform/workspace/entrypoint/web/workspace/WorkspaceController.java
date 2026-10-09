package br.com.portalmanager.platform.workspace.entrypoint.web.workspace;

import br.com.portalmanager.platform.workspace.entrypoint.web.workspace.request.CreateWorkspaceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.workspace.request.UpdateWorkspaceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.workspace.response.WorkspaceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import br.com.portalmanager.platform.workspace.core.workspace.facade.WorkspaceFacade;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceFacade facade;

    public WorkspaceController(WorkspaceFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<WorkspaceResponse> create(@RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse response = facade.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{identifier}")
    public WorkspaceResponse findByIdentifier(@PathVariable String identifier) {
        return facade.findByIdentifier(identifier);
    }

    @GetMapping
    public List<WorkspaceResponse> findAll(
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(required = false) String typeName,
            @RequestParam(required = false) String tagName
    ) {
        return facade.findAll(active, typeName, tagName);
    }

    @PutMapping("/{identifier}")
    public WorkspaceResponse update(
            @PathVariable String identifier,
            @RequestBody UpdateWorkspaceRequest request
    ) {
        return facade.update(identifier, request);
    }

    @PostMapping("/{identifier}/inactivate")
    public ResponseEntity<Void> inactivate(@PathVariable String identifier) {
        facade.inactivate(identifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/restore")
    public WorkspaceResponse restore(@PathVariable String identifier) {
        return facade.restore(identifier);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        facade.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
