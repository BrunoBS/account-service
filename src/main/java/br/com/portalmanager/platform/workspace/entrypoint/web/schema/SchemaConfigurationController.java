package br.com.portalmanager.platform.workspace.entrypoint.web.schema;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.operations.configuration.SchemaConfigurationService;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.CreateSchemaConfigurationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.UpdateSchemaConfigurationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.response.SchemaConfigurationResponse;
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
    public ResponseEntity<SchemaConfigurationResponse> create(@RequestBody CreateSchemaConfigurationRequest request) {
        var input = request == null ? null : request.toInput();
        return ResponseEntity.status(HttpStatus.CREATED).body(SchemaConfigurationResponse.from(service.create(input)));
    }

    @GetMapping
    public List<SchemaConfigurationResponse> findAll() {
        return service.findAll().stream().map(SchemaConfigurationResponse::from).toList();
    }

    @GetMapping("/{identifier}")
    public SchemaConfigurationResponse find(@PathVariable String identifier) {
        return SchemaConfigurationResponse.from(service.findByIdentifier(identifier));
    }

    @PutMapping("/{identifier}")
    public SchemaConfigurationResponse update(@PathVariable String identifier, @RequestBody UpdateSchemaConfigurationRequest request) {
        var input = request == null ? null : request.toInput();
        return SchemaConfigurationResponse.from(service.update(identifier, input));
    }

    @PatchMapping("/{identifier}/activate")
    public SchemaConfigurationResponse activate(@PathVariable String identifier) {
        return SchemaConfigurationResponse.from(service.activate(identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    public SchemaConfigurationResponse inactivate(@PathVariable String identifier) {
        return SchemaConfigurationResponse.from(service.inactivate(identifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        service.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
