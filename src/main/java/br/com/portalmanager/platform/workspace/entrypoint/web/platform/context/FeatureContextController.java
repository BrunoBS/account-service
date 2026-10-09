package br.com.portalmanager.platform.workspace.entrypoint.web.platform.context;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.request.*;import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.response.FeatureContextResponse;import br.com.portalmanager.platform.workspace.feature.platform.facade.FeatureContextFacade;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequestMapping("/api/v1/platform/contexts") public class FeatureContextController {private final FeatureContextFacade facade;public FeatureContextController(FeatureContextFacade f){facade=f;}
@PostMapping public ResponseEntity<FeatureContextResponse> create(@RequestBody CreateFeatureContextRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(FeatureContextResponse.from(facade.create(r.toInput())));}
@GetMapping("/{identifier}") public FeatureContextResponse findByIdentifier(@PathVariable String identifier){return FeatureContextResponse.from(facade.findByIdentifier(identifier));}
@GetMapping public List<FeatureContextResponse> findAll(){return facade.findAll().stream().map(FeatureContextResponse::from).toList();}
@PutMapping("/{identifier}") public FeatureContextResponse update(@PathVariable String identifier,@RequestBody UpdateFeatureContextRequest r){return FeatureContextResponse.from(facade.update(identifier,r.toInput()));}
@PatchMapping("/{identifier}/activate") public FeatureContextResponse activate(@PathVariable String identifier){return FeatureContextResponse.from(facade.activate(identifier));}
@PatchMapping("/{identifier}/inactivate") public FeatureContextResponse inactivate(@PathVariable String identifier){return FeatureContextResponse.from(facade.inactivate(identifier));}
@DeleteMapping("/{identifier}") public ResponseEntity<Void> delete(@PathVariable String identifier){facade.delete(identifier);return ResponseEntity.noContent().build();}}
