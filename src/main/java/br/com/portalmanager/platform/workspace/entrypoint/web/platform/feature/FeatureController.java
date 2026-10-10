package br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.context.response.FeatureContextResponse;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request.*;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.response.FeatureResponse;
import br.com.portalmanager.platform.workspace.feature.platform.facade.FeatureFacade;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/platform/features")
public class FeatureController {

    private final FeatureFacade facade;

    public FeatureController(FeatureFacade f) {
        facade = f;
    }

    @PostMapping
    public ResponseEntity<FeatureResponse> create(AuthorizationContext context, @RequestBody CreateFeatureRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            FeatureResponse.from(facade.create(context, r.toInput()))
        );
    }

    @GetMapping("/{identifier}")
    public FeatureResponse findByIdentifier(AuthorizationContext context, @PathVariable String identifier) {
        return FeatureResponse.from(facade.findByIdentifier(context, identifier));
    }

    @GetMapping
    public List<FeatureResponse> findAll(
        AuthorizationContext context,
        @RequestParam(required = false) String contextCode
    ) {
        return facade.findAll(context, contextCode).stream().map(FeatureResponse::from).toList();
    }

    @GetMapping("/{identifier}/contexts")
    public List<FeatureContextResponse> findContexts(AuthorizationContext context, @PathVariable String identifier) {
        return facade.findContexts(context, identifier).stream().map(FeatureContextResponse::from).toList();
    }

    @PutMapping("/{identifier}")
    public FeatureResponse update(
        AuthorizationContext context,
        @PathVariable String identifier,
        @RequestBody UpdateFeatureRequest r
    ) {
        return FeatureResponse.from(facade.update(context, identifier, r.toInput()));
    }

    @PatchMapping("/{identifier}/activate")
    public FeatureResponse activate(AuthorizationContext context, @PathVariable String identifier) {
        return FeatureResponse.from(facade.activate(context, identifier));
    }

    @PatchMapping("/{identifier}/inactivate")
    public FeatureResponse inactivate(AuthorizationContext context, @PathVariable String identifier) {
        return FeatureResponse.from(facade.inactivate(context, identifier));
    }

    @PostMapping("/{identifier}/contexts/{contextIdentifier}")
    public FeatureResponse associateContext(
        AuthorizationContext context,
        @PathVariable String identifier,
        @PathVariable String contextIdentifier
    ) {
        return FeatureResponse.from(facade.associateContext(context, identifier, contextIdentifier));
    }

    @DeleteMapping("/{identifier}/contexts/{contextIdentifier}")
    public FeatureResponse removeContext(
        AuthorizationContext context,
        @PathVariable String identifier,
        @PathVariable String contextIdentifier
    ) {
        return FeatureResponse.from(facade.removeContext(context, identifier, contextIdentifier));
    }

    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> delete(AuthorizationContext context, @PathVariable String identifier) {
        facade.delete(context, identifier);
        return ResponseEntity.noContent().build();
    }
}
