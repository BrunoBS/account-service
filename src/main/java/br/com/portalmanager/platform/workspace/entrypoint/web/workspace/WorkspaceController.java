package br.com.portalmanager.platform.workspace.entrypoint.web.workspace;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.model.FindAllWorkspacesInput;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceCommandService;
import br.com.portalmanager.platform.workspace.core.workspace.usecase.operations.WorkspaceQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.workspace.request.CreateWorkspaceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.workspace.request.UpdateWorkspaceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.workspace.response.WorkspaceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces")
public class WorkspaceController {

    private final WorkspaceCommandService commandService;
    private final WorkspaceQueryService queryService;

    public WorkspaceController(
            WorkspaceCommandService commandService,
            WorkspaceQueryService queryService
    ) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    @AuthorizationRequired(level = AuthorizationLevel.OPEN)
    @Auditable(
            resource = "WORKSPACE",
            action = "INSERT",
            resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "identifier")
    )
    public ResponseEntity<WorkspaceResponse> create(@RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse response = WorkspaceResponse.from(commandService.create(request.toInput()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public WorkspaceResponse findByIdentifier(@PathVariable String identifier) {
        return WorkspaceResponse.from(queryService.findByIdentifier(identifier));
    }

    @GetMapping
    @AuthorizationRequired(level = AuthorizationLevel.OPEN)
    public List<WorkspaceResponse> findAll(
            @RequestParam(defaultValue = "true") Boolean active,
            @RequestParam(required = false) String typeName,
            @RequestParam(required = false) String tagName
    ) {
        return queryService.findAll(new FindAllWorkspacesInput(active, typeName, tagName)).stream()
                .map(WorkspaceResponse::from)
                .toList();
    }

    @PutMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(
            resource = "WORKSPACE",
            action = "UPDATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public WorkspaceResponse update(
            @PathVariable String identifier,
            @RequestBody UpdateWorkspaceRequest request
    ) {
        return WorkspaceResponse.from(commandService.update(identifier, request.toInput()));
    }

    @PostMapping("/{identifier}/inactivate")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(
            resource = "WORKSPACE",
            action = "INACTIVATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public ResponseEntity<Void> inactivate(@PathVariable String identifier) {
        commandService.inactivate(identifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/restore")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(
            resource = "WORKSPACE",
            action = "RESTORE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public WorkspaceResponse restore(@PathVariable String identifier) {
        return WorkspaceResponse.from(commandService.restore(identifier));
    }

    @DeleteMapping("/{identifier}")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(
            resource = "WORKSPACE",
            action = "DELETE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        commandService.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
