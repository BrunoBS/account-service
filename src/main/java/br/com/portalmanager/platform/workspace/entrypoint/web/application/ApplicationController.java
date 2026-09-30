package br.com.portalmanager.platform.workspace.entrypoint.web.application;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.schemavalidation.web.annotation.ValidateResourceSchema;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationCommandService;
import br.com.portalmanager.platform.workspace.core.application.usecase.operations.ApplicationQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.application.request.CreateApplicationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.application.request.UpdateApplicationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.application.response.ApplicationResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.application.response.ApplicationSummaryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/applications")
public class ApplicationController {
    private final ApplicationCommandService command;
    private final ApplicationQueryService query;

    public ApplicationController(ApplicationCommandService command,
                                 ApplicationQueryService query) {
        this.command = command;
        this.query = query;
    }

    @PostMapping
    @ValidateResourceSchema(type = "APPLICATION", code = "application")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(resource = "APPLICATION", action = "INSERT", resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "identifier"))
    public ResponseEntity<ApplicationResponse> create(@PathVariable String workspaceIdentifier,
                                                       @RequestBody CreateApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApplicationResponse.from(command.create(workspaceIdentifier, request.toInput())));
    }

    @GetMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public ApplicationResponse find(@PathVariable String workspaceIdentifier, @PathVariable String identifier) {
        return ApplicationResponse.from(query.findByIdentifier(workspaceIdentifier, identifier));
    }

    @GetMapping
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public List<ApplicationResponse> findAll(@PathVariable String workspaceIdentifier,
                                              @RequestParam(defaultValue = "true") Boolean active,
                                              @RequestParam(required = false) String tagName) {
        return query.findAll(workspaceIdentifier, active, tagName).stream().map(ApplicationResponse::from).toList();
    }

    @GetMapping("/summary")
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public List<ApplicationSummaryResponse> summary(@PathVariable String workspaceIdentifier) {
        return query.summary(workspaceIdentifier).stream().map(ApplicationSummaryResponse::from).toList();
    }

    @PutMapping("/{identifier}")
    @ValidateResourceSchema(type = "APPLICATION", code = "application")
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    @Auditable(resource = "APPLICATION", action = "UPDATE", resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier"))
    public ApplicationResponse update(@PathVariable String workspaceIdentifier,
                                      @PathVariable String identifier,
                                      @RequestBody UpdateApplicationRequest request) {
        return ApplicationResponse.from(command.update(workspaceIdentifier, identifier, request.toInput()));
    }

    @PostMapping("/{identifier}/inactivate")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(resource = "APPLICATION", action = "INACTIVATE", resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier"))
    public ResponseEntity<Void> inactivate(@PathVariable String workspaceIdentifier, @PathVariable String identifier) {
        command.inactivate(workspaceIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/restore")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(resource = "APPLICATION", action = "RESTORE", resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier"))
    public ApplicationResponse restore(@PathVariable String workspaceIdentifier, @PathVariable String identifier) {
        return ApplicationResponse.from(command.restore(workspaceIdentifier, identifier));
    }

    @DeleteMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(resource = "APPLICATION", action = "DELETE", resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier"))
    public ResponseEntity<Void> delete(@PathVariable String workspaceIdentifier, @PathVariable String identifier) {
        command.delete(workspaceIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }
}
