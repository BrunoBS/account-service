package br.com.portalmanager.platform.workspace.entrypoint.web.schema;

import br.com.portalmanager.platform.workspace.foundation.schema.facade.SchemaConfigurationFacade;

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
public class SchemaConfigurationController {
    private final SchemaConfigurationFacade facade;

    public SchemaConfigurationController(SchemaConfigurationFacade facade) { this.facade = facade; }

    @PostMapping

    public ResponseEntity<SchemaConfigurationResponse> create(@RequestBody CreateSchemaConfigurationRequest request) {
        return facade.create(request);
    }

    @GetMapping

    public List<SchemaConfigurationResponse> findAll() {
        return facade.findAll();
    }

    @GetMapping("/{identifier}")

    public SchemaConfigurationResponse find(@PathVariable String identifier) {
        return facade.find(identifier);
    }

    @PutMapping("/{identifier}")

    public SchemaConfigurationResponse update(@PathVariable String identifier, @RequestBody UpdateSchemaConfigurationRequest request) {
        return facade.update(identifier, request);
    }

    @PatchMapping("/{identifier}/activate")

    public SchemaConfigurationResponse activate(@PathVariable String identifier) {
        return facade.activate(identifier);
    }

    @PatchMapping("/{identifier}/inactivate")

    public SchemaConfigurationResponse inactivate(@PathVariable String identifier) {
        return facade.inactivate(identifier);
    }

    @DeleteMapping("/{identifier}")

    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        return facade.delete(identifier);
    }
}
