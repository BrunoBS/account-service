package br.com.portalmanager.platform.workspace.entrypoint.web.publisher;
import br.com.portalmanager.platform.workspace.core.publisher.facade.PublisherFacade;import br.com.portalmanager.platform.workspace.entrypoint.web.publisher.request.*;import br.com.portalmanager.platform.workspace.entrypoint.web.publisher.response.PublisherResponse;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequestMapping("/api/v1/publishers") public class PublisherController {private final PublisherFacade facade;public PublisherController(PublisherFacade f){facade=f;}
@PostMapping public ResponseEntity<PublisherResponse> create(@RequestBody CreatePublisherRequest r){return ResponseEntity.status(HttpStatus.CREATED).body(PublisherResponse.from(facade.create(r.toInput())));}
@GetMapping public List<PublisherResponse> list(@RequestParam(defaultValue="true") Boolean active,@RequestParam(required=false) String scope){return facade.list(active,scope).stream().map(PublisherResponse::from).toList();}
@GetMapping("/{identifier}") public PublisherResponse find(@PathVariable String identifier){return PublisherResponse.from(facade.find(identifier));}
@PutMapping("/{identifier}") public PublisherResponse update(@PathVariable String identifier,@RequestBody UpdatePublisherRequest r){return PublisherResponse.from(facade.update(identifier,r.toInput()));}
@PostMapping("/{identifier}/inactivate") public ResponseEntity<Void> inactivate(@PathVariable String identifier){facade.inactivate(identifier);return ResponseEntity.noContent().build();}
@PostMapping("/{identifier}/restore") public PublisherResponse restore(@PathVariable String identifier){return PublisherResponse.from(facade.restore(identifier));}
@DeleteMapping("/{identifier}") public ResponseEntity<Void> delete(@PathVariable String identifier){facade.delete(identifier);return ResponseEntity.noContent().build();}}
