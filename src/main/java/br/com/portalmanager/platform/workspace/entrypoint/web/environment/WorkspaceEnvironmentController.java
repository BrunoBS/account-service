package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.workspace.core.environment.facade.WorkspaceEnvironmentFacade;

import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentCommandService;
import br.com.portalmanager.platform.workspace.core.environment.usecase.operations.environment.EnvironmentQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.CreateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.UpdateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentTreeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/environments")
public class WorkspaceEnvironmentController {
    private final WorkspaceEnvironmentFacade facade;

    public WorkspaceEnvironmentController(WorkspaceEnvironmentFacade facade) { this.facade = facade; }

    @PostMapping

    public ResponseEntity<EnvironmentResponse> create(@PathVariable String workspaceIdentifier, @RequestBody CreateEnvironmentRequest request) {
        return facade.create(workspaceIdentifier, request);
    }

    @GetMapping

    public List<EnvironmentResponse> list(@PathVariable String workspaceIdentifier, @RequestParam(defaultValue = "true") Boolean active) {
        return facade.list(workspaceIdentifier, active);
    }

    @GetMapping("/{identifier}")

    public EnvironmentResponse find(@PathVariable String workspaceIdentifier, @PathVariable String identifier) {
        return facade.find(workspaceIdentifier, identifier);
    }

    @GetMapping("/roots")

    public List<EnvironmentResponse> roots(@PathVariable String workspaceIdentifier) {
        return facade.roots(workspaceIdentifier);
    }

    @GetMapping("/{identifier}/children")

    public List<EnvironmentResponse> children(@PathVariable String workspaceIdentifier, @PathVariable String identifier) {
        return facade.children(workspaceIdentifier, identifier);
    }

    @GetMapping("/tree")

    public List<EnvironmentTreeResponse> tree(@PathVariable String workspaceIdentifier) {
        return facade.tree(workspaceIdentifier);
    }

    @PutMapping("/{identifier}")

    public EnvironmentResponse update(@PathVariable String workspaceIdentifier, @PathVariable String identifier,
                                      @RequestBody UpdateEnvironmentRequest request) {
        return facade.update(workspaceIdentifier, identifier, request);
    }

    @PostMapping("/{identifier}/inactivate")

    public ResponseEntity<Void> inactivate(@PathVariable String workspaceIdentifier, @PathVariable String identifier) {
        return facade.inactivate(workspaceIdentifier, identifier);
    }

    @PostMapping("/{identifier}/restore")

    public EnvironmentResponse restore(@PathVariable String workspaceIdentifier, @PathVariable String identifier) {
        return facade.restore(workspaceIdentifier, identifier);
    }

    @DeleteMapping("/{identifier}")

    public ResponseEntity<Void> delete(@PathVariable String workspaceIdentifier, @PathVariable String identifier) {
        return facade.delete(workspaceIdentifier, identifier);
    }
}
