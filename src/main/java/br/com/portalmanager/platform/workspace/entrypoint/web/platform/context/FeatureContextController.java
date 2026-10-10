package br.com.portalmanager.platform.workspace.entrypoint.web.platform.context;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.request.*;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.response.FeatureContextResponse;
import br.com.portalmanager.platform.workspace.feature.platform.facade.FeatureContextFacade;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/platform/contexts")
public class FeatureContextController {

    private final FeatureContextFacade facade;

    public FeatureContextController(FeatureContextFacade f) {
        facade = f;
    }

    @PostMapping
    public ResponseEntity<FeatureContextResponse> create(
        AuthorizationContext context,
        @RequestBody CreateFeatureContextRequest r
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            FeatureContextResponse.from(facade.create(context, r.toInput()))
        );
    }

    @GetMapping("/{identifier}")
    public FeatureContextResponse findByIdentifier(AuthorizationContext context, @PathVariable String identifier) {
        return FeatureContextResponse.from(facade.findByIdentifier(context, identifier));
    }

    @GetMapping
    public List<FeatureContextResponse> findAll(AuthorizationContext context) {
        return facade.findAll(context).stream().map(FeatureContextResponse::from).toList();
    }

    @PutMapping("/{identifier}")
    public FeatureContextResponse update(
        AuthorizationContext context,
        @PathVariable String identifier,
        @RequestBody UpdateFeatureContextRequest r
    ) {
        return FeatureContextResponse.from(facade.update(context, identifier, r.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    public FeatureContextResponse activate(AuthorizationContext context, @PathVariable String identifier) {
        return FeatureContextResponse.from(facade.activate(context, identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    public FeatureContextResponse inactivate(AuthorizationContext context, @PathVariable String identifier) {
        return FeatureContextResponse.from(facade.inactivate(context, identifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(AuthorizationContext context, @PathVariable String identifier) {
        facade.delete(context, identifier);
        return ResponseEntity.noContent().build();
    }
}
