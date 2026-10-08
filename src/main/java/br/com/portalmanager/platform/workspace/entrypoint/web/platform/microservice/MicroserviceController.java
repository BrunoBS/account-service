package br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationRequired;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.request.CreateMicroserviceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.request.UpdateMicroserviceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.response.MicroserviceResponse;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceCommandService;
import br.com.portalmanager.platform.workspace.feature.platform.usecase.operations.microservice.MicroserviceQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform/microservices")
@AuthorizationRequired(level = AuthorizationLevel.OWNER)
public class MicroserviceController {

    private final MicroserviceCommandService commandService;
    private final MicroserviceQueryService queryService;

    public MicroserviceController(MicroserviceCommandService commandService, MicroserviceQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<MicroserviceResponse> create(@RequestBody CreateMicroserviceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MicroserviceResponse.from(commandService.create(request.toInput())));
    }

    @GetMapping("/{identifier}")
    public MicroserviceResponse findByIdentifier(@PathVariable String identifier) {
        return MicroserviceResponse.from(queryService.findByIdentifier(identifier));
    }

    @GetMapping
    public List<MicroserviceResponse> findAll() {
        return queryService.findAll().stream().map(MicroserviceResponse::from).toList();
    }

    @PutMapping("/{identifier}")
    public MicroserviceResponse update(
            @PathVariable String identifier,
            @RequestBody UpdateMicroserviceRequest request
    ) {
        return MicroserviceResponse.from(commandService.update(identifier, request.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    public MicroserviceResponse activate(@PathVariable String identifier) {
        return MicroserviceResponse.from(commandService.activate(identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    public MicroserviceResponse inactivate(@PathVariable String identifier) {
        return MicroserviceResponse.from(commandService.inactivate(identifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        commandService.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
