package br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.request.*;import br.com.portalmanager.platform.workspace.entrypoint.web.platform.microservice.response.MicroserviceResponse;import br.com.portalmanager.platform.workspace.feature.platform.facade.MicroserviceFacade;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequestMapping("/api/v1/platform/microservices") public class MicroserviceController {private final MicroserviceFacade facade;public MicroserviceController(MicroserviceFacade f){facade=f;}
@PostMapping public ResponseEntity<MicroserviceResponse> create(@RequestBody CreateMicroserviceRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(MicroserviceResponse.from(facade.create(r.toInput())));}
@GetMapping("/{identifier}") public MicroserviceResponse findByIdentifier(@PathVariable String identifier){return MicroserviceResponse.from(facade.findByIdentifier(identifier));}
@GetMapping public List<MicroserviceResponse> findAll(){return facade.findAll().stream().map(MicroserviceResponse::from).toList();}
@PutMapping("/{identifier}") public MicroserviceResponse update(@PathVariable String identifier,@RequestBody UpdateMicroserviceRequest r){return MicroserviceResponse.from(facade.update(identifier,r.toInput()));}
@PatchMapping("/{identifier}/activate") public MicroserviceResponse activate(@PathVariable String identifier){return MicroserviceResponse.from(facade.activate(identifier));}
@PatchMapping("/{identifier}/inactivate") public MicroserviceResponse inactivate(@PathVariable String identifier){return MicroserviceResponse.from(facade.inactivate(identifier));}
@DeleteMapping("/{identifier}") public ResponseEntity<Void> delete(@PathVariable String identifier){facade.delete(identifier);return ResponseEntity.noContent().build();}}
