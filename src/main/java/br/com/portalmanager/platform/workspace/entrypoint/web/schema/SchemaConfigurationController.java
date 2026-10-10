package br.com.portalmanager.platform.workspace.entrypoint.web.schema;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.request.*;
import br.com.portalmanager.platform.workspace.entrypoint.web.schema.response.SchemaConfigurationResponse;
import br.com.portalmanager.platform.workspace.foundation.schema.facade.SchemaConfigurationFacade;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/schema-configurations")
public class SchemaConfigurationController {

    private final SchemaConfigurationFacade facade;

    public SchemaConfigurationController(SchemaConfigurationFacade f) {
        facade = f;
    }

    @PostMapping
    public ResponseEntity<SchemaConfigurationResponse> create(
        AuthorizationContext context,
        @RequestBody CreateSchemaConfigurationRequest r
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            SchemaConfigurationResponse.from(facade.create(context, r == null ? null : r.toInput()))
        );
    }

    @GetMapping
    public List<SchemaConfigurationResponse> findAll(AuthorizationContext context) {
        return facade.findAll(context).stream().map(SchemaConfigurationResponse::from).toList();
    }

    @GetMapping("/{identifier}")
    public SchemaConfigurationResponse find(AuthorizationContext context, @PathVariable String identifier) {
        return SchemaConfigurationResponse.from(facade.find(context, identifier));
    }

    @PutMapping("/{identifier}")
    public SchemaConfigurationResponse update(
        AuthorizationContext context,
        @PathVariable String identifier,
        @RequestBody UpdateSchemaConfigurationRequest r
    ) {
        return SchemaConfigurationResponse.from(facade.update(context, identifier, r == null ? null : r.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    public SchemaConfigurationResponse activate(AuthorizationContext context, @PathVariable String identifier) {
        return SchemaConfigurationResponse.from(facade.activate(context, identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    public SchemaConfigurationResponse inactivate(AuthorizationContext context, @PathVariable String identifier) {
        return SchemaConfigurationResponse.from(facade.inactivate(context, identifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(AuthorizationContext context, @PathVariable String identifier) {
        facade.delete(context, identifier);
        return ResponseEntity.noContent().build();
    }
}
