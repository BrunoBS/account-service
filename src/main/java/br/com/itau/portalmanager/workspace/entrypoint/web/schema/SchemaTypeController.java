package br.com.itau.portalmanager.workspace.entrypoint.web.schema;

import br.com.itau.portalmanager.workspace.entrypoint.web.schema.request.CreateSchemaTypeRequest;
import br.com.itau.portalmanager.workspace.entrypoint.web.schema.response.SchemaTypeResponse;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.SchemaTypeCommandService;
import br.com.itau.portalmanager.workspace.foundation.schema.usecase.SchemaTypeQueryService;
import br.com.portalmanager.platform.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.audit.annotation.AuditField;
import br.com.portalmanager.platform.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.audit.annotation.Auditable;
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
}
