package br.com.portalmanager.platform.workspace.entrypoint.web.schema;

import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.CreateSchemaRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.CreateSchemaVersionRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.UpdateSchemaRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.response.SchemaResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.response.SchemaVersionResponse;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.schema.SchemaCommandService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.schema.SchemaQueryService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.version.SchemaVersionCommandService;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schemas")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class PlatformSchemaController {

    private final SchemaCommandService commandService;
    private final SchemaQueryService queryService;
    private final SchemaVersionCommandService versionCommandService;

    public PlatformSchemaController(
            SchemaCommandService commandService,
            SchemaQueryService queryService,
            SchemaVersionCommandService versionCommandService
    ) {
        this.commandService = commandService;
        this.queryService = queryService;
        this.versionCommandService = versionCommandService;
    }

    @PostMapping
    @Auditable(
            resource = "SCHEMA",
            action = "INSERT",
            resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "identifier")
    )
    public ResponseEntity<SchemaResponse> create(@RequestBody CreateSchemaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SchemaResponse.from(commandService.createPlatform(request.toPlatformInput())));
    }

    @GetMapping
    public List<SchemaResponse> findAll() {
        return queryService.findPlatform().stream().map(SchemaResponse::from).toList();
    }

    @GetMapping("/{identifier}")
    public SchemaResponse findByIdentifier(@PathVariable String identifier) {
        return SchemaResponse.from(queryService.findPlatformByIdentifier(identifier));
    }

    @PutMapping("/{identifier}")
    @Auditable(
            resource = "SCHEMA",
            action = "UPDATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public SchemaResponse update(
            @PathVariable String identifier,
            @RequestBody UpdateSchemaRequest request
    ) {
        return SchemaResponse.from(commandService.updatePlatform(identifier, request.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    public SchemaResponse activate(@PathVariable String identifier) {
        return SchemaResponse.from(commandService.activatePlatform(identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    public SchemaResponse inactivate(@PathVariable String identifier) {
        return SchemaResponse.from(commandService.inactivatePlatform(identifier));
    }

    @DeleteMapping("/{identifier}")
    @Auditable(
            resource = "SCHEMA",
            action = "DELETE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public ResponseEntity<Void> quarantine(@PathVariable String identifier) {
        commandService.quarantinePlatform(identifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/versions")
    @Auditable(
            resource = "SCHEMA_VERSION",
            action = "INSERT",
            resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "identifier")
    )
    public ResponseEntity<SchemaVersionResponse> createVersion(
            @PathVariable String identifier,
            @RequestBody CreateSchemaVersionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SchemaVersionResponse.from(
                        versionCommandService.createPlatformDraft(identifier, request.toInput())
                ));
    }

    @DeleteMapping("/{identifier}/versions/{versionIdentifier}")
    public ResponseEntity<Void> deleteDraft(
            @PathVariable String identifier,
            @PathVariable String versionIdentifier
    ) {
        versionCommandService.deletePlatformDraft(identifier, versionIdentifier);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{identifier}/versions")
    public List<SchemaVersionResponse> findVersions(@PathVariable String identifier) {
        return queryService.findPlatformVersions(identifier).stream()
                .map(SchemaVersionResponse::from)
                .toList();
    }

    @PatchMapping("/{identifier}/versions/{versionIdentifier}/publish")
    @Auditable(
            resource = "SCHEMA_VERSION",
            action = "PUBLISH",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "versionIdentifier")
    )
    public SchemaVersionResponse publish(
            @PathVariable String identifier,
            @PathVariable String versionIdentifier
    ) {
        return SchemaVersionResponse.from(
                versionCommandService.publishPlatform(identifier, versionIdentifier)
        );
    }
}
