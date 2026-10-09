package br.com.portalmanager.platform.workspace.entrypoint.web.schema;

import br.com.portalmanager.platform.workspace.foundation.schema.facade.PlatformSchemaFacade;

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
@RequestMapping("/api/v1/schemas")
public class PlatformSchemaController {
    private final PlatformSchemaFacade facade;


    public PlatformSchemaController(PlatformSchemaFacade facade) { this.facade = facade; }

    @PostMapping

    public ResponseEntity<SchemaResponse> create(@RequestBody CreateSchemaRequest request) {
        return facade.create(request);
    }

    @GetMapping

    public List<SchemaResponse> findAll() {
        return facade.findAll();
    }

    @GetMapping("/{identifier}")

    public SchemaResponse findByIdentifier(@PathVariable String identifier) {
        return facade.findByIdentifier(identifier);
    }

    @PutMapping("/{identifier}")

    public SchemaResponse update(
            @PathVariable String identifier,
            @RequestBody UpdateSchemaRequest request
    ) {
        return facade.update(identifier, request);
    }

    @PatchMapping("/{identifier}/activate")

    public SchemaResponse activate(@PathVariable String identifier) {
        return facade.activate(identifier);
    }

    @PatchMapping("/{identifier}/inactivate")

    public SchemaResponse inactivate(@PathVariable String identifier) {
        return facade.inactivate(identifier);
    }

    @DeleteMapping("/{identifier}")

    public ResponseEntity<Void> quarantine(@PathVariable String identifier) {
        return facade.quarantine(identifier);
    }

    @PostMapping("/{identifier}/versions")

    public ResponseEntity<SchemaVersionResponse> createVersion(
            @PathVariable String identifier,
            @RequestBody CreateSchemaVersionRequest request
    ) {
        return facade.createVersion(identifier, request);
    }

    @DeleteMapping("/{identifier}/versions/{versionIdentifier}")

    public ResponseEntity<Void> deleteDraft(
            @PathVariable String identifier,
            @PathVariable String versionIdentifier
    ) {
        return facade.deleteDraft(identifier, versionIdentifier);
    }

    @GetMapping("/{identifier}/versions")

    public List<SchemaVersionResponse> findVersions(@PathVariable String identifier) {
        return facade.findVersions(identifier);
    }

    @PatchMapping("/{identifier}/versions/{versionIdentifier}/publish")

    public SchemaVersionResponse publish(
            @PathVariable String identifier,
            @PathVariable String versionIdentifier
    ) {
        return facade.publish(identifier, versionIdentifier);
    }
}
