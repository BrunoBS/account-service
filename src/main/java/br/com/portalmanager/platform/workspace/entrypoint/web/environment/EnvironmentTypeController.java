package br.com.portalmanager.platform.workspace.entrypoint.web.environment;
import br.com.portalmanager.platform.workspace.core.environment.facade.EnvironmentTypeFacade;import br.com.portalmanager.platform.workspace.entrypoint.web.environment.request.*;import br.com.portalmanager.platform.workspace.entrypoint.web.environment.response.EnvironmentTypeResponse;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequestMapping("/api/v1/environment-types") public class EnvironmentTypeController {private final EnvironmentTypeFacade facade;public EnvironmentTypeController(EnvironmentTypeFacade f){facade=f;}
@PostMapping public ResponseEntity<EnvironmentTypeResponse> create(@RequestBody CreateEnvironmentTypeRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(EnvironmentTypeResponse.from(facade.create(r==null?null:r.toInput())));}
@GetMapping public List<EnvironmentTypeResponse> list(){return facade.list().stream().map(EnvironmentTypeResponse::from).toList();}
@GetMapping("/{identifier}") public EnvironmentTypeResponse find(@PathVariable String identifier){return EnvironmentTypeResponse.from(facade.find(identifier));}
@PutMapping("/{identifier}") public EnvironmentTypeResponse update(@PathVariable String identifier,@RequestBody UpdateEnvironmentTypeRequest r){return EnvironmentTypeResponse.from(facade.update(identifier,r==null?null:r.toInput()));}
@PostMapping("/{identifier}/inactivate") public ResponseEntity<Void> inactivate(@PathVariable String identifier){facade.inactivate(identifier);return ResponseEntity.noContent().build();}
@PostMapping("/{identifier}/restore") public EnvironmentTypeResponse restore(@PathVariable String identifier){return EnvironmentTypeResponse.from(facade.restore(identifier));}
@DeleteMapping("/{identifier}") public ResponseEntity<Void> delete(@PathVariable String identifier){facade.delete(identifier);return ResponseEntity.noContent().build();}}
