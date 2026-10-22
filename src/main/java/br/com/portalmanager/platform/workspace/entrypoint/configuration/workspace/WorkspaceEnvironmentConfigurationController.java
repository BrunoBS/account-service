package br.com.portalmanager.platform.workspace.entrypoint.configuration.workspace;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.core.workspace.environmentconfiguration.facade.WorkspaceEnvironmentConfigurationFacade;
import br.com.portalmanager.platform.workspace.entrypoint.configuration.workspace.request.*;
import br.com.portalmanager.platform.workspace.entrypoint.configuration.workspace.response.WorkspaceEnvironmentConfigurationResponse;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/environment-configurations")
public class WorkspaceEnvironmentConfigurationController {
    private final WorkspaceEnvironmentConfigurationFacade facade;

    public WorkspaceEnvironmentConfigurationController(
            WorkspaceEnvironmentConfigurationFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<WorkspaceEnvironmentConfigurationResponse> create(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @RequestBody CreateWorkspaceEnvironmentConfigurationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        WorkspaceEnvironmentConfigurationResponse.from(
                                facade.create(context, workspaceIdentifier, request.toInput())));
    }

    @PutMapping("/{identifier}")
    public WorkspaceEnvironmentConfigurationResponse update(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier,
            @RequestBody UpdateWorkspaceEnvironmentConfigurationRequest request) {
        return WorkspaceEnvironmentConfigurationResponse.from(
                facade.update(context, workspaceIdentifier, identifier, request.toInput()));
    }

    @GetMapping("/{identifier}")
    public WorkspaceEnvironmentConfigurationResponse find(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier) {
        return WorkspaceEnvironmentConfigurationResponse.from(
                facade.find(context, workspaceIdentifier, identifier));
    }

    @GetMapping
    public PagedModel<WorkspaceEnvironmentConfigurationResponse> findAll(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @RequestParam(required = false) String lifecycle,
            Pageable pageable) {
        return new PagedModel<>(
                facade.findAll(context, workspaceIdentifier, lifecycle, pageable)
                        .map(WorkspaceEnvironmentConfigurationResponse::from));
    }

    @PostMapping("/{identifier}/inactivate")
    public ResponseEntity<Void> inactivate(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier) {
        facade.inactivate(context, workspaceIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> quarantine(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier) {
        facade.quarantine(context, workspaceIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }
}
