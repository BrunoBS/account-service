package br.com.portalmanager.platform.workspace.entrypoint.web.platform.context;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.request.CreateContextRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.request.UpdateContextRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.response.ContextResponse;
import br.com.portalmanager.platform.workspace.feature.platform.facade.context.ContextFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform/contexts")
public class ContextController {

    private final ContextFacade facade;

    public ContextController(ContextFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<ContextResponse> create(
        AuthorizationContext authorizationContext,
        @RequestBody CreateContextRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            ContextResponse.from(facade.create(authorizationContext, request.toInput()))
        );
    }

    @GetMapping("/{contextIdentifier}")
    public ContextResponse findByIdentifier(AuthorizationContext authorizationContext, @PathVariable String contextIdentifier) {
        return ContextResponse.from(facade.findByIdentifier(authorizationContext, contextIdentifier));
    }

    @GetMapping
    public Page<ContextResponse> findAll(
        AuthorizationContext authorizationContext,
        @RequestParam(required = false) String lifecycle,
        @RequestParam(required = false) String featureIdentifier,
        @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return facade.findAll(authorizationContext, featureIdentifier, lifecycle, pageable).map(ContextResponse::from);
    }

    @PutMapping("/{contextIdentifier}")
    public ContextResponse update(
        AuthorizationContext authorizationContext,
        @PathVariable String contextIdentifier,
        @RequestBody UpdateContextRequest request
    ) {
        return ContextResponse.from(facade.update(authorizationContext, contextIdentifier, request.toInput()));
    }

    @PatchMapping("/{contextIdentifier}/activate")
    public ContextResponse activate(AuthorizationContext authorizationContext, @PathVariable String contextIdentifier) {
        return ContextResponse.from(facade.activate(authorizationContext, contextIdentifier));
    }

    @PatchMapping("/{contextIdentifier}/inactivate")
    public ContextResponse inactivate(AuthorizationContext authorizationContext, @PathVariable String contextIdentifier) {
        return ContextResponse.from(facade.inactivate(authorizationContext, contextIdentifier));
    }

    @DeleteMapping("/{contextIdentifier}")
    public ResponseEntity<Void> purge(AuthorizationContext authorizationContext, @PathVariable String contextIdentifier) {
        facade.purge(authorizationContext, contextIdentifier);
        return ResponseEntity.noContent().build();
    }
}
