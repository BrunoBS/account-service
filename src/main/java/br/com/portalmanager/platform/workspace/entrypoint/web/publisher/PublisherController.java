package br.com.portalmanager.platform.workspace.entrypoint.web.publisher;

import br.com.portalmanager.platform.library.authorization.annotation.AuthorizationAccessPolicy;
import br.com.portalmanager.platform.library.authorization.model.AuthorizationLevel;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.operations.PublisherCommandService;
import br.com.portalmanager.platform.workspace.core.publisher.usecase.operations.PublisherQueryService;
import br.com.portalmanager.platform.workspace.entrypoint.web.publisher.request.CreatePublisherRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.publisher.request.UpdatePublisherRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.publisher.response.PublisherResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/publishers")
@AuthorizationAccessPolicy(read = AuthorizationLevel.OPEN, write = AuthorizationLevel.OWNER)
public class PublisherController {
    private final PublisherCommandService command;
    private final PublisherQueryService query;
    public PublisherController(PublisherCommandService command, PublisherQueryService query) {
        this.command = command;
        this.query = query;
    }
    @PostMapping
    public ResponseEntity<PublisherResponse> create(@RequestBody CreatePublisherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(PublisherResponse.from(command.create(request.toInput())));
    }
    @GetMapping
    public List<PublisherResponse> list(@RequestParam(defaultValue = "true") Boolean active,
                                        @RequestParam(required = false) String scope) {
        return query.list(active, scope).stream().map(PublisherResponse::from).toList();
    }
    @GetMapping("/{identifier}")
    public PublisherResponse find(@PathVariable String identifier) { return PublisherResponse.from(query.find(identifier)); }
    @PutMapping("/{identifier}")
    public PublisherResponse update(@PathVariable String identifier, @RequestBody UpdatePublisherRequest request) {
        return PublisherResponse.from(command.update(identifier, request.toInput()));
    }
    @PostMapping("/{identifier}/inactivate")
    public ResponseEntity<Void> inactivate(@PathVariable String identifier) {
        command.inactivate(identifier);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/{identifier}/restore")
    public PublisherResponse restore(@PathVariable String identifier) {
        return PublisherResponse.from(command.restore(identifier));
    }
    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(@PathVariable String identifier) {
        command.delete(identifier);
        return ResponseEntity.noContent().build();
    }
}
