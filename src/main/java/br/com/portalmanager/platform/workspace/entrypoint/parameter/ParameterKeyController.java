package br.com.portalmanager.platform.workspace.entrypoint.parameter;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.request.CreateParameterKeyRequest;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.request.PublishParameterKeysRequest;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.request.UpdateParameterKeyRequest;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.response.ParameterKeyMetadataResponse;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.response.ParameterKeyResponse;
import br.com.portalmanager.platform.workspace.feature.parameter.facade.DynamicParameterFacade;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dynamic-parameters")
public class ParameterKeyController {
    private final DynamicParameterFacade facade;

    public ParameterKeyController(DynamicParameterFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<ParameterKeyMetadataResponse> create(
            AuthorizationContext context, @RequestBody CreateParameterKeyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ParameterKeyMetadataResponse.from(facade.create(context, request.toInput())));
    }

    @GetMapping("/{identifier}")
    public ParameterKeyResponse findByIdentifier(
            AuthorizationContext context, @PathVariable UUID identifier) {
        return ParameterKeyResponse.from(facade.findByIdentifier(context, identifier.toString()));
    }

    @GetMapping
    public Page<ParameterKeyResponse> findAll(
            AuthorizationContext context,
            @RequestParam(required = false) UUID microserviceIdentifier,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "true") boolean active,
            @RequestParam(required = false) String key,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return facade.findAll(
                        context,
                        microserviceIdentifier == null ? null : microserviceIdentifier.toString(),
                        status,
                        type,
                        active,
                        key,
                        pageable)
                .map(ParameterKeyResponse::from);
    }

    @PutMapping("/{identifier}")
    public ParameterKeyMetadataResponse update(
            AuthorizationContext context,
            @PathVariable UUID identifier,
            @RequestBody UpdateParameterKeyRequest request) {
        return ParameterKeyMetadataResponse.from(
                facade.update(context, identifier.toString(), request.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    public ParameterKeyMetadataResponse activate(
            AuthorizationContext context, @PathVariable UUID identifier) {
        return ParameterKeyMetadataResponse.from(facade.activate(context, identifier.toString()));
    }

    @PostMapping("/publish")
    public ResponseEntity<Void> publish(
            AuthorizationContext context, @RequestBody PublishParameterKeysRequest request) {
        facade.publish(context, request.toInput());
        return ResponseEntity.accepted().build();
    }

    @PatchMapping("/{identifier}/inactivate")
    public ParameterKeyMetadataResponse inactivate(
            AuthorizationContext context, @PathVariable UUID identifier) {
        return ParameterKeyMetadataResponse.from(facade.inactivate(context, identifier.toString()));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(
            AuthorizationContext context, @PathVariable UUID identifier) {
        facade.delete(context, identifier.toString());
        return ResponseEntity.noContent().build();
    }
}
