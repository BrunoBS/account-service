package br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request.CreateFeatureRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.request.UpdateFeatureRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.response.FeatureResponse;
import br.com.portalmanager.platform.workspace.feature.platform.facade.feature.FeatureFacade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/platform/features")
public class FeatureController {

    private final FeatureFacade facade;

    public FeatureController(FeatureFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<FeatureResponse> create(AuthorizationContext authorizationContext, @RequestBody CreateFeatureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                FeatureResponse.from(facade.create(authorizationContext, request.toInput()))
        );
    }

    @GetMapping("/{featureIdentifier}")
    public FeatureResponse findByIdentifier(AuthorizationContext authorizationContext, @PathVariable String featureIdentifier) {
        return FeatureResponse.from(facade.findByIdentifier(authorizationContext, featureIdentifier));
    }

    @GetMapping
    public Page<FeatureResponse> findAll(
            AuthorizationContext authorizationContext,
            @RequestParam(required = false) String lifecycle,
            @RequestParam(required = false) String contextIdentifier,
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return facade.findAll(authorizationContext, contextIdentifier, lifecycle, pageable)
                .map(FeatureResponse::from);
    }

    @PutMapping("/{featureIdentifier}")
    public FeatureResponse update(
            AuthorizationContext authorizationContext,
            @PathVariable String featureIdentifier,
            @RequestBody UpdateFeatureRequest request
    ) {
        return FeatureResponse.from(facade.update(authorizationContext, featureIdentifier, request.toInput()));
    }

    @PatchMapping("/{featureIdentifier}/activate")
    public FeatureResponse activate(AuthorizationContext authorizationContext, @PathVariable String featureIdentifier) {
        return FeatureResponse.from(facade.activate(authorizationContext, featureIdentifier));
    }

    @PatchMapping("/{featureIdentifier}/inactivate")
    public FeatureResponse inactivate(AuthorizationContext authorizationContext, @PathVariable String featureIdentifier) {
        return FeatureResponse.from(facade.inactivate(authorizationContext, featureIdentifier));
    }
}
