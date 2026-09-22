package br.com.itau.portalmanager.workspace.input.web.workspace;

import br.com.itau.portalmanager.workspace.core.workspace.usecase.WorkspaceCommandService;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.model.FindAllWorkspacesInput;
import br.com.itau.portalmanager.workspace.core.workspace.usecase.WorkspaceQueryService;
import br.com.itau.portalmanager.workspace.input.web.workspace.request.CreateWorkspaceRequest;
import br.com.itau.portalmanager.workspace.input.web.workspace.request.UpdateWorkspaceRequest;
import br.com.itau.portalmanager.workspace.input.web.workspace.response.WorkspaceResponse;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.audit.annotation.AuditField;
import br.com.portalmanager.platform.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.audit.annotation.Auditable;
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
            resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "id")
    )
    public ResponseEntity<WorkspaceResponse> create(@RequestBody CreateWorkspaceRequest request) {
        WorkspaceResponse response = WorkspaceResponse.from(commandService.create(request.toInput()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{workspaceId}")
    @AuthorizationRequired(level = AuthorizationLevel.DEV)
    public WorkspaceResponse findById(@PathVariable Long workspaceId) {
        return WorkspaceResponse.from(queryService.findById(workspaceId));
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

    @PutMapping("/{workspaceId}")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(
            resource = "WORKSPACE",
            action = "UPDATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "workspaceId")
    )
    public WorkspaceResponse update(
            @PathVariable Long workspaceId,
            @RequestBody UpdateWorkspaceRequest request
    ) {
        return WorkspaceResponse.from(commandService.update(workspaceId, request.toInput()));
    }

    @DeleteMapping("/{workspaceId}")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(
            resource = "WORKSPACE",
            action = "DELETE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "workspaceId")
    )
    public ResponseEntity<Void> inactivate(@PathVariable Long workspaceId) {
        commandService.inactivate(workspaceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{workspaceId}/restore")
    @AuthorizationRequired(level = AuthorizationLevel.ADM)
    @Auditable(
            resource = "WORKSPACE",
            action = "RESTORE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "workspaceId")
    )
    public WorkspaceResponse restore(@PathVariable Long workspaceId) {
        return WorkspaceResponse.from(commandService.restore(workspaceId));
    }
}
