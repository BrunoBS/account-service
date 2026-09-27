package br.com.portalmanager.platform.workspace.entrypoint.web.schema;

import br.com.portalmanager.platform.library.audit.annotation.AuditField;
import br.com.portalmanager.platform.library.audit.annotation.AuditFieldSource;
import br.com.portalmanager.platform.library.audit.annotation.Auditable;
import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaConfigurationInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaConfigurationOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaConfigurationInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.configuration.SchemaConfigurationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schema-configurations")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class SchemaConfigurationController {
    private final SchemaConfigurationService service;
    public SchemaConfigurationController(SchemaConfigurationService service) { this.service = service; }

    @PostMapping
    @Auditable(resource = "SCHEMA_CONFIGURATION", action = "INSERT",
            resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "identifier"))
    public ResponseEntity<SchemaConfigurationOutput> create(@RequestBody CreateSchemaConfigurationInput request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public List<SchemaConfigurationOutput> findAll() { return service.findAll(); }

    @GetMapping("/{identifier}")
    public SchemaConfigurationOutput find(@PathVariable String identifier) {
        return service.findByIdentifier(identifier);
    }

    @PutMapping("/{identifier}")
    @Auditable(resource = "SCHEMA_CONFIGURATION", action = "UPDATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier"))
    public SchemaConfigurationOutput update(@PathVariable String identifier, @RequestBody UpdateSchemaConfigurationInput request) {
        return service.update(identifier, request);
    }

    @PatchMapping("/{identifier}/activate")
    public SchemaConfigurationOutput activate(@PathVariable String identifier) { return service.activate(identifier); }

    @PatchMapping("/{identifier}/inactivate")
    public SchemaConfigurationOutput inactivate(@PathVariable String identifier) { return service.inactivate(identifier); }

    @DeleteMapping("/{identifier}")
    @Auditable(resource = "SCHEMA_CONFIGURATION", action = "DELETE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier"))
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        service.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
