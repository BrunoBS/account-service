package br.com.portalmanager.platform.workspace.entrypoint.configuration.application;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.core.application.environmentconfiguration.facade.ApplicationEnvironmentConfigurationFacade;
import br.com.portalmanager.platform.workspace.entrypoint.configuration.application.request.*;
import br.com.portalmanager.platform.workspace.entrypoint.configuration.application.response.ApplicationEnvironmentConfigurationResponse;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedModel;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        "/api/v1/workspaces/{workspaceIdentifier}/applications/{applicationIdentifier}/environment-configurations")
public class ApplicationEnvironmentConfigurationController {
    private final ApplicationEnvironmentConfigurationFacade facade;

    public ApplicationEnvironmentConfigurationController(
            ApplicationEnvironmentConfigurationFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<ApplicationEnvironmentConfigurationResponse> create(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @RequestBody CreateApplicationEnvironmentConfigurationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApplicationEnvironmentConfigurationResponse.from(
                                facade.create(
                                        context,
                                        workspaceIdentifier,
                                        applicationIdentifier,
                                        request.toInput())));
    }

    @PutMapping("/{identifier}")
    public ApplicationEnvironmentConfigurationResponse update(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String identifier,
            @RequestBody UpdateApplicationEnvironmentConfigurationRequest request) {
        return ApplicationEnvironmentConfigurationResponse.from(
                facade.update(
                        context,
                        workspaceIdentifier,
                        applicationIdentifier,
                        identifier,
                        request.toInput()));
    }

    @GetMapping("/{identifier}")
    public ApplicationEnvironmentConfigurationResponse find(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String identifier) {
        return ApplicationEnvironmentConfigurationResponse.from(
                facade.find(context, workspaceIdentifier, applicationIdentifier, identifier));
    }

    @GetMapping
    public PagedModel<ApplicationEnvironmentConfigurationResponse> findAll(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @RequestParam(required = false) String lifecycle,
            Pageable pageable) {
        return new PagedModel<>(
                facade.findAll(
                                context,
                                workspaceIdentifier,
                                applicationIdentifier,
                                lifecycle,
                                pageable)
                        .map(ApplicationEnvironmentConfigurationResponse::from));
    }

    @PostMapping("/{identifier}/inactivate")
    public ResponseEntity<Void> inactivate(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String identifier) {
        facade.inactivate(context, workspaceIdentifier, applicationIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> quarantine(
            AuthorizationContext context,
            @PathVariable String workspaceIdentifier,
            @PathVariable String applicationIdentifier,
            @PathVariable String identifier) {
        facade.quarantine(context, workspaceIdentifier, applicationIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }
}
