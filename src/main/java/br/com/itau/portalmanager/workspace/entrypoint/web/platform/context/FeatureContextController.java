package br.com.itau.portalmanager.workspace.entrypoint.web.platform.context;

import br.com.itau.portalmanager.workspace.entrypoint.web.platform.context.request.CreateFeatureContextRequest;
import br.com.itau.portalmanager.workspace.entrypoint.web.platform.context.request.UpdateFeatureContextRequest;
import br.com.itau.portalmanager.workspace.entrypoint.web.platform.context.response.FeatureContextResponse;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.context.FeatureContextCommandService;
import br.com.itau.portalmanager.workspace.feature.platform.usecase.context.FeatureContextQueryService;
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
@RequestMapping("/api/v1/platform/contexts")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class FeatureContextController {

    private final FeatureContextCommandService commandService;
    private final FeatureContextQueryService queryService;

    public FeatureContextController(
            FeatureContextCommandService commandService,
            FeatureContextQueryService queryService
    ) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    @Auditable(
            resource = "FEATURE_CONTEXT",
            action = "INSERT",
            resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "identifier")
    )
    public ResponseEntity<FeatureContextResponse> create(@RequestBody CreateFeatureContextRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FeatureContextResponse.from(commandService.create(request.toInput())));
    }

    @GetMapping("/{identifier}")
    public FeatureContextResponse findByIdentifier(@PathVariable String identifier) {
        return FeatureContextResponse.from(queryService.findByIdentifier(identifier));
    }

    @GetMapping
    public List<FeatureContextResponse> findAll() {
        return queryService.findAll().stream().map(FeatureContextResponse::from).toList();
    }

    @PutMapping("/{identifier}")
    @Auditable(
            resource = "FEATURE_CONTEXT",
            action = "UPDATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public FeatureContextResponse update(
            @PathVariable String identifier,
            @RequestBody UpdateFeatureContextRequest request
    ) {
        return FeatureContextResponse.from(commandService.update(identifier, request.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    @Auditable(
            resource = "FEATURE_CONTEXT",
            action = "ACTIVATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public FeatureContextResponse activate(@PathVariable String identifier) {
        return FeatureContextResponse.from(commandService.activate(identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    @Auditable(
            resource = "FEATURE_CONTEXT",
            action = "INACTIVATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public FeatureContextResponse inactivate(@PathVariable String identifier) {
        return FeatureContextResponse.from(commandService.inactivate(identifier));
    }

    @DeleteMapping("/{identifier}")
    @Auditable(
            resource = "FEATURE_CONTEXT",
            action = "DELETE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        commandService.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
