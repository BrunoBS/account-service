package br.com.portalmanager.platform.workspace.entrypoint.web.schema;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.CreateSchemaTypeRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.UpdateSchemaTypeRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.response.SchemaTypeResponse;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schematype.SchemaTypeCommandService;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.schematype.SchemaTypeQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schema-types")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class SchemaTypeController {

    private final SchemaTypeCommandService commandService;
    private final SchemaTypeQueryService queryService;

    public SchemaTypeController(
            SchemaTypeCommandService commandService,
            SchemaTypeQueryService queryService
    ) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    @Auditable(
            resource = "SCHEMA_TYPE",
            action = "INSERT",
            resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "identifier")
    )
    public ResponseEntity<SchemaTypeResponse> create(@RequestBody CreateSchemaTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SchemaTypeResponse.from(commandService.create(request.toInput())));
    }

    @GetMapping
    public List<SchemaTypeResponse> findAll() {
        return queryService.findAll().stream().map(SchemaTypeResponse::from).toList();
    }

    @GetMapping("/{identifier}")
    public SchemaTypeResponse findByIdentifier(@PathVariable String identifier) {
        return SchemaTypeResponse.from(queryService.findByIdentifier(identifier));
    }

    @PutMapping("/{identifier}")
    @Auditable(
            resource = "SCHEMA_TYPE",
            action = "UPDATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public SchemaTypeResponse update(
            @PathVariable String identifier,
            @RequestBody UpdateSchemaTypeRequest request
    ) {
        return SchemaTypeResponse.from(commandService.update(identifier, request.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    @Auditable(
            resource = "SCHEMA_TYPE",
            action = "ACTIVATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public SchemaTypeResponse activate(@PathVariable String identifier) {
        return SchemaTypeResponse.from(commandService.activate(identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    @Auditable(
            resource = "SCHEMA_TYPE",
            action = "INACTIVATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public SchemaTypeResponse inactivate(@PathVariable String identifier) {
        return SchemaTypeResponse.from(commandService.inactivate(identifier));
    }

    @DeleteMapping("/{identifier}")
    @Auditable(
            resource = "SCHEMA_TYPE",
            action = "DELETE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        commandService.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
