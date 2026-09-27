package br.com.portalmanager.platform.workspace.entrypoint.web.schema;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.CreateSchemaTypeInput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.SchemaTypeOutput;
import br.com.portalmanager.platform.workspace.foundation.schema.usecase.model.UpdateSchemaTypeInput;
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
    public ResponseEntity<SchemaTypeOutput> create(@RequestBody CreateSchemaTypeInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commandService.create(input));
    }

    @GetMapping
    public List<SchemaTypeOutput> findAll() {
        return queryService.findAll();
    }

    @GetMapping("/{identifier}")
    public SchemaTypeOutput findByIdentifier(@PathVariable String identifier) {
        return queryService.findByIdentifier(identifier);
    }

    @PutMapping("/{identifier}")
    public SchemaTypeOutput update(
            @PathVariable String identifier,
            @RequestBody UpdateSchemaTypeInput input
    ) {
        return commandService.update(identifier, input);
    }

    @PatchMapping("/{identifier}/activate")
    public SchemaTypeOutput activate(@PathVariable String identifier) {
        return commandService.activate(identifier);
    }

    @PatchMapping("/{identifier}/inactivate")
    public SchemaTypeOutput inactivate(@PathVariable String identifier) {
        return commandService.inactivate(identifier);
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        commandService.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
