package br.com.portalmanager.platform.workspace.entrypoint.web.schema;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.CreateSchemaRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.CreateSchemaVersionRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.UpdateSchemaRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.response.SchemaResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.response.SchemaVersionResponse;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema.SchemaCommandService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schema.SchemaQueryService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.version.SchemaVersionCommandService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/schemas")
@AuthorizationRequired(level = AuthorizationLevel.ADM)
public class WorkspaceSchemaController {

    private final SchemaCommandService commandService;
    private final SchemaQueryService queryService;
    private final SchemaVersionCommandService versionCommandService;

    public WorkspaceSchemaController(
            SchemaCommandService commandService,
            SchemaQueryService queryService,
            SchemaVersionCommandService versionCommandService
    ) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.versionCommandService = versionCommandService;
    }

    @PostMapping
    public ResponseEntity<SchemaResponse> create(
            @PathVariable String workspaceIdentifier,
            @RequestBody CreateSchemaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SchemaResponse.from(
                        commandService.createWorkspace(request.toWorkspaceInput(workspaceIdentifier))
                ));
    }

    @GetMapping
    public List<SchemaResponse> findAll(@PathVariable String workspaceIdentifier) {
        return queryService.findWorkspace(workspaceIdentifier).stream()
                .map(SchemaResponse::from)
                .toList();
    }

    @GetMapping("/{identifier}")
    public SchemaResponse findByIdentifier(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier
    ) {
        return SchemaResponse.from(queryService.findWorkspaceByIdentifier(workspaceIdentifier, identifier));
    }

    @PutMapping("/{identifier}")
    public SchemaResponse update(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier,
            @RequestBody UpdateSchemaRequest request
    ) {
        return SchemaResponse.from(commandService.updateWorkspace(workspaceIdentifier, identifier, request.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    public SchemaResponse activate(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier
    ) {
        return SchemaResponse.from(commandService.activateWorkspace(workspaceIdentifier, identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    public SchemaResponse inactivate(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier
    ) {
        return SchemaResponse.from(commandService.inactivateWorkspace(workspaceIdentifier, identifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> quarantine(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier
    ) {
        commandService.quarantineWorkspace(workspaceIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/versions")
    public ResponseEntity<SchemaVersionResponse> createVersion(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier,
            @RequestBody CreateSchemaVersionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SchemaVersionResponse.from(
                        versionCommandService.createWorkspaceDraft(workspaceIdentifier, identifier, request.toInput())
                ));
    }

    @DeleteMapping("/{identifier}/versions/{versionIdentifier}")
    public ResponseEntity<Void> deleteDraft(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier,
            @PathVariable String versionIdentifier
    ) {
        versionCommandService.deleteWorkspaceDraft(workspaceIdentifier, identifier, versionIdentifier);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{identifier}/versions")
    public List<SchemaVersionResponse> findVersions(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier
    ) {
        return queryService.findWorkspaceVersions(workspaceIdentifier, identifier).stream()
                .map(SchemaVersionResponse::from)
                .toList();
    }

    @PatchMapping("/{identifier}/versions/{versionIdentifier}/publish")
    public SchemaVersionResponse publish(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier,
            @PathVariable String versionIdentifier
    ) {
        return SchemaVersionResponse.from(
                versionCommandService.publishWorkspace(workspaceIdentifier, identifier, versionIdentifier)
        );
    }
}
