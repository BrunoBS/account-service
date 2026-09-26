package br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature;

import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.response.FeatureContextResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request.CreateFeatureRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request.UpdateFeatureRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.response.FeatureResponse;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.feature.FeatureQueryService;
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
@RequestMapping("/api/v1/platform/features")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class FeatureController {

    private final FeatureCommandService commandService;
    private final FeatureQueryService queryService;

    public FeatureController(FeatureCommandService commandService, FeatureQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    @Auditable(
            resource = "FEATURE",
            action = "INSERT",
            resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "identifier")
    )
    public ResponseEntity<FeatureResponse> create(@RequestBody CreateFeatureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FeatureResponse.from(commandService.create(request.toInput())));
    }

    @GetMapping("/{identifier}")
    public FeatureResponse findByIdentifier(@PathVariable String identifier) {
        return FeatureResponse.from(queryService.findByIdentifier(identifier));
    }

    @GetMapping
    public List<FeatureResponse> findAll(@RequestParam(required = false) String contextCode) {
        return (contextCode == null ? queryService.findAll() : queryService.findByContext(contextCode))
                .stream()
                .map(FeatureResponse::from)
                .toList();
    }

    @GetMapping("/{identifier}/contexts")
    public List<FeatureContextResponse> findContexts(@PathVariable String identifier) {
        return queryService.findContexts(identifier).stream().map(FeatureContextResponse::from).toList();
    }

    @PutMapping("/{identifier}")
    @Auditable(
            resource = "FEATURE",
            action = "UPDATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public FeatureResponse update(
            @PathVariable String identifier,
            @RequestBody UpdateFeatureRequest request
    ) {
        return FeatureResponse.from(commandService.update(identifier, request.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    @Auditable(
            resource = "FEATURE",
            action = "ACTIVATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public FeatureResponse activate(@PathVariable String identifier) {
        return FeatureResponse.from(commandService.activate(identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    @Auditable(
            resource = "FEATURE",
            action = "INACTIVATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public FeatureResponse inactivate(@PathVariable String identifier) {
        return FeatureResponse.from(commandService.inactivate(identifier));
    }

    @PostMapping("/{identifier}/contexts/{contextIdentifier}")
    @Auditable(
            resource = "FEATURE",
            action = "UPDATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public FeatureResponse associateContext(
            @PathVariable String identifier,
            @PathVariable String contextIdentifier
    ) {
        return FeatureResponse.from(commandService.associateContext(identifier, contextIdentifier));
    }

    @DeleteMapping("/{identifier}/contexts/{contextIdentifier}")
    @Auditable(
            resource = "FEATURE",
            action = "UPDATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public FeatureResponse removeContext(
            @PathVariable String identifier,
            @PathVariable String contextIdentifier
    ) {
        return FeatureResponse.from(commandService.removeContext(identifier, contextIdentifier));
    }

    @DeleteMapping("/{identifier}")
    @Auditable(
            resource = "FEATURE",
            action = "DELETE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        commandService.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
