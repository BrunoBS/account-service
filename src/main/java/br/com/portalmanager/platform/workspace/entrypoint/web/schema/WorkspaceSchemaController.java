package br.com.portalmanager.platform.workspace.entrypoint.web.schema;

import br.com.portalmanager.platform.workspace.foundation.schema.facade.WorkspaceSchemaFacade;

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
public class WorkspaceSchemaController {
    private final WorkspaceSchemaFacade facade;


    public WorkspaceSchemaController(WorkspaceSchemaFacade facade) { this.facade = facade; }

    @PostMapping

    public ResponseEntity<SchemaResponse> create(
            @PathVariable String workspaceIdentifier,
            @RequestBody CreateSchemaRequest request
    ) {
        return facade.create(workspaceIdentifier, request);
    }

    @GetMapping

    public List<SchemaResponse> findAll(@PathVariable String workspaceIdentifier) {
        return facade.findAll(workspaceIdentifier);
    }

    @GetMapping("/{identifier}")

    public SchemaResponse findByIdentifier(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier
    ) {
        return facade.findByIdentifier(workspaceIdentifier, identifier);
    }

    @PutMapping("/{identifier}")

    public SchemaResponse update(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier,
            @RequestBody UpdateSchemaRequest request
    ) {
        return facade.update(workspaceIdentifier, identifier, request);
    }

    @PatchMapping("/{identifier}/activate")

    public SchemaResponse activate(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier
    ) {
        return facade.activate(workspaceIdentifier, identifier);
    }

    @PatchMapping("/{identifier}/inactivate")

    public SchemaResponse inactivate(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier
    ) {
        return facade.inactivate(workspaceIdentifier, identifier);
    }

    @DeleteMapping("/{identifier}")

    public ResponseEntity<Void> quarantine(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier
    ) {
        return facade.quarantine(workspaceIdentifier, identifier);
    }

    @PostMapping("/{identifier}/versions")

    public ResponseEntity<SchemaVersionResponse> createVersion(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier,
            @RequestBody CreateSchemaVersionRequest request
    ) {
        return facade.createVersion(workspaceIdentifier, identifier, request);
    }

    @DeleteMapping("/{identifier}/versions/{versionIdentifier}")

    public ResponseEntity<Void> deleteDraft(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier,
            @PathVariable String versionIdentifier
    ) {
        return facade.deleteDraft(workspaceIdentifier, identifier, versionIdentifier);
    }

    @GetMapping("/{identifier}/versions")

    public List<SchemaVersionResponse> findVersions(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier
    ) {
        return facade.findVersions(workspaceIdentifier, identifier);
    }

    @PatchMapping("/{identifier}/versions/{versionIdentifier}/publish")

    public SchemaVersionResponse publish(
            @PathVariable String workspaceIdentifier,
            @PathVariable String identifier,
            @PathVariable String versionIdentifier
    ) {
        return facade.publish(workspaceIdentifier, identifier, versionIdentifier);
    }
}
