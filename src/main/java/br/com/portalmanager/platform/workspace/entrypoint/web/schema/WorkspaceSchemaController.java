package br.com.portalmanager.platform.workspace.entrypoint.web.schema;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.*;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.response.*;
import br.com.portalmanager.platform.workspace.foundation.schema.facade.WorkspaceSchemaFacade;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/schemas")
public class WorkspaceSchemaController {

    private final WorkspaceSchemaFacade facade;

    public WorkspaceSchemaController(WorkspaceSchemaFacade f) {
        facade = f;
    }

    @PostMapping
    public ResponseEntity<SchemaResponse> create(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @RequestBody CreateSchemaRequest r
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            SchemaResponse.from(facade.create(context, r.toWorkspaceInput(workspaceIdentifier)))
        );
    }

    @GetMapping
    public List<SchemaResponse> findAll(AuthorizationContext context, @PathVariable String workspaceIdentifier) {
        return facade.findAll(context, workspaceIdentifier).stream().map(SchemaResponse::from).toList();
    }

    @GetMapping("/{identifier}")
    public SchemaResponse findByIdentifier(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier
    ) {
        return SchemaResponse.from(facade.findByIdentifier(context, workspaceIdentifier, identifier));
    }

    @PutMapping("/{identifier}")
    public SchemaResponse update(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier,
        @RequestBody UpdateSchemaRequest r
    ) {
        return SchemaResponse.from(facade.update(context, workspaceIdentifier, identifier, r.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    public SchemaResponse activate(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier
    ) {
        return SchemaResponse.from(facade.activate(context, workspaceIdentifier, identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    public SchemaResponse inactivate(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier
    ) {
        return SchemaResponse.from(facade.inactivate(context, workspaceIdentifier, identifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> quarantine(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier
    ) {
        facade.quarantine(context, workspaceIdentifier, identifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/versions")
    public ResponseEntity<SchemaVersionResponse> createVersion(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier,
        @RequestBody CreateSchemaVersionRequest r
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            SchemaVersionResponse.from(facade.createVersion(context, workspaceIdentifier, identifier, r.toInput()))
        );
    }

    @DeleteMapping("/{identifier}/versions/{versionIdentifier}")
    public ResponseEntity<Void> deleteDraft(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier,
        @PathVariable String versionIdentifier
    ) {
        facade.deleteDraft(context, workspaceIdentifier, identifier, versionIdentifier);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{identifier}/versions")
    public List<SchemaVersionResponse> findVersions(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier
    ) {
        return facade
            .findVersions(context, workspaceIdentifier, identifier)
            .stream()
            .map(SchemaVersionResponse::from)
            .toList();
    }

    @PatchMapping("/{identifier}/versions/{versionIdentifier}/publish")
    public SchemaVersionResponse publish(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier,
        @PathVariable String identifier,
        @PathVariable String versionIdentifier
    ) {
        return SchemaVersionResponse.from(facade.publish(context, workspaceIdentifier, identifier, versionIdentifier));
    }
}
