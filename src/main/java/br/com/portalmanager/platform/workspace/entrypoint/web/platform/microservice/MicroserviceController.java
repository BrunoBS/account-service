package br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice;

import br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.request.CreateMicroserviceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.request.UpdateMicroserviceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.response.MicroserviceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import br.com.portalmanager.platform.workspace.feature.platform.facade.MicroserviceFacade;

@RestController
@RequestMapping("/api/v1/platform/microservices")
public class MicroserviceController {

    private final MicroserviceFacade facade;
    public MicroserviceController(MicroserviceFacade facade) { this.facade = facade; }
    @PostMapping
    public ResponseEntity<MicroserviceResponse> create(@RequestBody CreateMicroserviceRequest request) { return ResponseEntity.status(HttpStatus.CREATED).body(facade.create(request)); }
    @GetMapping("/{identifier}")
    public MicroserviceResponse findByIdentifier(@PathVariable String identifier) { return facade.findByIdentifier(identifier); }
    @GetMapping
    public List<MicroserviceResponse> findAll() { return facade.findAll(); }
    @PutMapping("/{identifier}")
    public MicroserviceResponse update(@PathVariable String identifier, @RequestBody UpdateMicroserviceRequest request) { return facade.update(identifier, request); }
    @PatchMapping("/{identifier}/activate")
    public MicroserviceResponse activate(@PathVariable String identifier) { return facade.activate(identifier); }
    @PatchMapping("/{identifier}/inactivate")
    public MicroserviceResponse inactivate(@PathVariable String identifier) { return facade.inactivate(identifier); }
    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(@PathVariable String identifier) { facade.delete(identifier); return ResponseEntity.noContent().build(); }
}
