package br.com.portalmanager.platform.workspace.entrypoint.web.environment;
import br.com.portalmanager.platform.workspace.core.environment.facade.DefaultEnvironmentFacade;import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.*;import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentResponse;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequestMapping("/api/v1/environment-defaults") public class DefaultEnvironmentController {private final DefaultEnvironmentFacade facade;public DefaultEnvironmentController(DefaultEnvironmentFacade f){facade=f;}
@PostMapping public ResponseEntity<EnvironmentResponse> create(@RequestBody CreateEnvironmentRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(EnvironmentResponse.from(facade.create(r.toInput())));}
@GetMapping public List<EnvironmentResponse> list(@RequestParam(defaultValue="true") Boolean active){return facade.list(active).stream().map(EnvironmentResponse::from).toList();}
@GetMapping("/{identifier}") public EnvironmentResponse find(@PathVariable String identifier){return EnvironmentResponse.from(facade.find(identifier));}
@PutMapping("/{identifier}") public EnvironmentResponse update(@PathVariable String identifier,@RequestBody UpdateEnvironmentRequest r){return EnvironmentResponse.from(facade.update(identifier,r.toInput()));}
@PostMapping("/{identifier}/inactivate") public ResponseEntity<Void> inactivate(@PathVariable String identifier){facade.inactivate(identifier);return ResponseEntity.noContent().build();}
@PostMapping("/{identifier}/restore") public EnvironmentResponse restore(@PathVariable String identifier){return EnvironmentResponse.from(facade.restore(identifier));}
@DeleteMapping("/{identifier}") public ResponseEntity<Void> delete(@PathVariable String identifier){facade.delete(identifier);return ResponseEntity.noContent().build();}}
