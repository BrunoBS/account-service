package br.com.portalmanager.platform.workspace.entrypoint.web.publisher;

import br.com.portalmanager.platform.workspace.entrypoint.web.publisher.request.CreatePublisherRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.publisher.request.UpdatePublisherRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.publisher.response.PublisherResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import br.com.portalmanager.platform.workspace.core.publisher.facade.PublisherFacade;

@RestController
@RequestMapping("/api/v1/publishers")
public class PublisherController {
    private final PublisherFacade facade;
    public PublisherController(PublisherFacade facade) { this.facade = facade; }
    @PostMapping
        public ResponseEntity<PublisherResponse> create(@RequestBody CreatePublisherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(facade.create(request));
    }
    @GetMapping
    public List<PublisherResponse> list(@RequestParam(defaultValue = "true") Boolean active,
                                        @RequestParam(required = false) String scope) {
        return facade.list(active, scope);
    }
    @GetMapping("/{identifier}")
    public PublisherResponse find(@PathVariable String identifier) { return facade.find(identifier); }
    @PutMapping("/{identifier}")
        public PublisherResponse update(@PathVariable String identifier, @RequestBody UpdatePublisherRequest request) {
        return facade.update(identifier, request);
    }
    @PostMapping("/{identifier}/inactivate")
        public ResponseEntity<Void> inactivate(@PathVariable String identifier) {
        facade.inactivate(identifier);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{identifier}/restore")
        public PublisherResponse restore(@PathVariable String identifier) {
        return facade.restore(identifier);
    }
    @DeleteMapping("/{identifier}")
        public ResponseEntity<Void> delete(@PathVariable String identifier) {
        facade.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
