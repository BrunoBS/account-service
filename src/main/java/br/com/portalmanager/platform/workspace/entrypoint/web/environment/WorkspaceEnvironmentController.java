package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
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
    private final EnvironmentCommandService command;
    private final EnvironmentQueryService query;
    public WorkspaceEnvironmentController(EnvironmentCommandService command, EnvironmentQueryService query) {
        this.command = command;
        this.query = query;
    }

    @PostMapping
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(resource = "ENVIRONMENT", action = "INSERT", resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "identifier"))
    public ResponseEntity<EnvironmentResponse> create(@PathVariable String workspaceIdentifier, @RequestBody CreateEnvironmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(EnvironmentResponse.from(command.createCustom(workspaceIdentifier, request.toInput())));
    }

    @GetMapping
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public List<EnvironmentResponse> list(@PathVariable String workspaceIdentifier, @RequestParam(defaultValue = "true") Boolean active) {
        return query.listCustom(workspaceIdentifier, active).stream().map(EnvironmentResponse::from).toList();
    }

    @GetMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public EnvironmentResponse find(@PathVariable String workspaceIdentifier, @PathVariable String environmentIdentifier) {
        return EnvironmentResponse.from(query.findCustom(workspaceIdentifier, environmentIdentifier));
    }

    @GetMapping("/roots")
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public List<EnvironmentResponse> roots(@PathVariable String workspaceIdentifier) {
        return query.roots(workspaceIdentifier).stream().map(EnvironmentResponse::from).toList();
    }

    @GetMapping("/{identifier}/children")
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public List<EnvironmentResponse> children(@PathVariable String workspaceIdentifier, @PathVariable String environmentIdentifier) {
        return query.children(workspaceIdentifier, environmentIdentifier).stream().map(EnvironmentResponse::from).toList();
    }

    @GetMapping("/tree")
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public List<EnvironmentTreeResponse> tree(@PathVariable String workspaceIdentifier) {
        return query.tree(workspaceIdentifier).stream().map(EnvironmentTreeResponse::from).toList();
    }

    @PutMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(resource = "ENVIRONMENT", action = "UPDATE", resourceId = @AuditField(source = AuditFieldSource.PATH, field = "environmentIdentifier"))
    public EnvironmentResponse update(@PathVariable String workspaceIdentifier, @PathVariable String environmentIdentifier,
                                      @RequestBody UpdateEnvironmentRequest request) {
        return EnvironmentResponse.from(command.updateCustom(workspaceIdentifier, environmentIdentifier, request.toInput()));
    }

    @PostMapping("/{identifier}/inactivate")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(resource = "ENVIRONMENT", action = "INACTIVATE", resourceId = @AuditField(source = AuditFieldSource.PATH, field = "environmentIdentifier"))
    public ResponseEntity<Void> inactivate(@PathVariable String workspaceIdentifier, @PathVariable String environmentIdentifier) {
        command.inactivateCustom(workspaceIdentifier, environmentIdentifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/restore")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(resource = "ENVIRONMENT", action = "RESTORE", resourceId = @AuditField(source = AuditFieldSource.PATH, field = "environmentIdentifier"))
    public EnvironmentResponse restore(@PathVariable String workspaceIdentifier, @PathVariable String environmentIdentifier) {
        return EnvironmentResponse.from(command.restoreCustom(workspaceIdentifier, environmentIdentifier));
    }

    @DeleteMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(resource = "ENVIRONMENT", action = "DELETE", resourceId = @AuditField(source = AuditFieldSource.PATH, field = "environmentIdentifier"))
    public ResponseEntity<Void> delete(@PathVariable String workspaceIdentifier, @PathVariable String environmentIdentifier) {
        command.deleteCustom(workspaceIdentifier, environmentIdentifier);
        return ResponseEntity.noContent().build();
    }
}
