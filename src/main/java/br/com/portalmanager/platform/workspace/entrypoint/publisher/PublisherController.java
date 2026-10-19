package br.com.portalmanager.platform.workspace.entrypoint.publisher;


import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.core.publisher.facade.PublisherFacade;
import br.com.portalmanager.platform.workspace.entrypoint.publisher.request.*;
import br.com.portalmanager.platform.workspace.entrypoint.publisher.response.PublisherResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/publishers")
public class PublisherController {

    private final PublisherFacade facade;

    public PublisherController(PublisherFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<PublisherResponse> create(
        AuthorizationContext context,
        @RequestBody CreatePublisherRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            PublisherResponse.from(facade.create(context, request.toInput()))
        );
    }

    @GetMapping
    public PagedModel<PublisherResponse> findAll(
        AuthorizationContext context,
        @RequestParam(required = false) String lifecycle,
        @RequestParam(required = false) String scope,
        @PageableDefault(sort = "code") Pageable pageable
    ) {
        return new PagedModel<>(facade.findAll(context, lifecycle, scope, pageable).map(PublisherResponse::from));
    }

    @GetMapping("/{identifier}")
    public PublisherResponse find(AuthorizationContext context, @PathVariable String identifier) {
        return PublisherResponse.from(facade.find(context, identifier));
    }

    @PutMapping("/{identifier}")
    public PublisherResponse update(
        AuthorizationContext context,
        @PathVariable String identifier,
        @RequestBody UpdatePublisherRequest request
    ) {
        return PublisherResponse.from(facade.update(context, identifier, request.toInput()));
    }

    @PostMapping("/{identifier}/inactivate")
    public ResponseEntity<Void> inactivate(AuthorizationContext context, @PathVariable String identifier) {
        facade.inactivate(context, identifier);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{identifier}/restore")
    public PublisherResponse restore(AuthorizationContext context, @PathVariable String identifier) {
        return PublisherResponse.from(facade.restore(context, identifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(AuthorizationContext context, @PathVariable String identifier) {
        facade.delete(context, identifier);
        return ResponseEntity.noContent().build();
    }
}
