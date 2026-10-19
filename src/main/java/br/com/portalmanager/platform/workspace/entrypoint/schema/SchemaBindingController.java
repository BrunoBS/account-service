package br.com.portalmanager.platform.workspace.entrypoint.schema;


import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.*;
import br.com.portalmanager.platform.workspace.entrypoint.schema.response.SchemaBindingResponse;
import br.com.portalmanager.platform.workspace.foundation.schema.facade.binding.SchemaBindingFacade;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/schema-bindings")
public class SchemaBindingController {

    private final SchemaBindingFacade facade;

    public SchemaBindingController(SchemaBindingFacade f) {
        facade = f;
    }

    @PostMapping
    public ResponseEntity<SchemaBindingResponse> create(
        AuthorizationContext context,
        @RequestBody CreateSchemaBindingRequest r
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            SchemaBindingResponse.from(facade.create(context, r == null ? null : r.toInput()))
        );
    }

    @GetMapping
    public List<SchemaBindingResponse> findAll(
        AuthorizationContext context,
        @RequestParam(required = false) String lifecycle
    ) {
        return facade.findAll(context, lifecycle).stream().map(SchemaBindingResponse::from).toList();
    }

    @GetMapping("/{identifier}")
    public SchemaBindingResponse find(AuthorizationContext context, @PathVariable String identifier) {
        return SchemaBindingResponse.from(facade.find(context, identifier));
    }

    @PutMapping("/{identifier}")
    public SchemaBindingResponse update(
        AuthorizationContext context,
        @PathVariable String identifier,
        @RequestBody UpdateSchemaBindingRequest r
    ) {
        return SchemaBindingResponse.from(facade.update(context, identifier, r == null ? null : r.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    public SchemaBindingResponse activate(AuthorizationContext context, @PathVariable String identifier) {
        return SchemaBindingResponse.from(facade.activate(context, identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    public SchemaBindingResponse inactivate(AuthorizationContext context, @PathVariable String identifier) {
        return SchemaBindingResponse.from(facade.inactivate(context, identifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(AuthorizationContext context, @PathVariable String identifier) {
        facade.delete(context, identifier);
        return ResponseEntity.noContent().build();
    }
}
