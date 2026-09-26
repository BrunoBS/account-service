package br.com.portalmanager.platform.workspace.entrypoint.web.platform.service;

import br.com.portalmanager.platform.workspace.entrypoint.web.platform.service.request.CreateServiceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.service.request.UpdateServiceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.service.response.ServiceResponse;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.service.ServiceCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.service.ServiceQueryService;
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
@RequestMapping("/api/v1/platform/services")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class ServiceController {

    private final ServiceCommandService commandService;
    private final ServiceQueryService queryService;

    public ServiceController(ServiceCommandService commandService, ServiceQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    @Auditable(
            resource = "SERVICE",
            action = "INSERT",
            resourceId = @AuditField(source = AuditFieldSource.RESPONSE, field = "identifier")
    )
    public ResponseEntity<ServiceResponse> create(@RequestBody CreateServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ServiceResponse.from(commandService.create(request.toInput())));
    }

    @GetMapping("/{identifier}")
    public ServiceResponse findByIdentifier(@PathVariable String identifier) {
        return ServiceResponse.from(queryService.findByIdentifier(identifier));
    }

    @GetMapping
    public List<ServiceResponse> findAll() {
        return queryService.findAll().stream().map(ServiceResponse::from).toList();
    }

    @PutMapping("/{identifier}")
    @Auditable(
            resource = "SERVICE",
            action = "UPDATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public ServiceResponse update(
            @PathVariable String identifier,
            @RequestBody UpdateServiceRequest request
    ) {
        return ServiceResponse.from(commandService.update(identifier, request.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    @Auditable(
            resource = "SERVICE",
            action = "ACTIVATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public ServiceResponse activate(@PathVariable String identifier) {
        return ServiceResponse.from(commandService.activate(identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    @Auditable(
            resource = "SERVICE",
            action = "INACTIVATE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public ServiceResponse inactivate(@PathVariable String identifier) {
        return ServiceResponse.from(commandService.inactivate(identifier));
    }

    @DeleteMapping("/{identifier}")
    @Auditable(
            resource = "SERVICE",
            action = "DELETE",
            resourceId = @AuditField(source = AuditFieldSource.PATH, field = "identifier")
    )
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        commandService.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
