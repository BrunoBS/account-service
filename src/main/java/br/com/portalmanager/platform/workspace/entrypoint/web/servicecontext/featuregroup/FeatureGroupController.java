package br.com.portalmanager.platform.workspace.entrypoint.web.servicecontext.featuregroup;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.servicecontext.featuregroup.request.CreateFeatureGroupRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.servicecontext.featuregroup.request.UpdateFeatureGroupRequest;
import br.com.portalmanager.platform.workspace.entrypoint.web.servicecontext.featuregroup.response.FeatureGroupResponse;
import br.com.portalmanager.platform.workspace.feature.servicecontext.facade.featuregroup.FeatureGroupFacade;

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
@RequestMapping("/api/v1/service-contexts/feature-groups")
public class FeatureGroupController {

    private final FeatureGroupFacade facade;

    public FeatureGroupController(FeatureGroupFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public ResponseEntity<FeatureGroupResponse> create(
            AuthorizationContext authorizationContext, @RequestBody CreateFeatureGroupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(FeatureGroupResponse.from(facade.create(authorizationContext, request.toInput())));
    }

    @GetMapping("/{featureGroupIdentifier}")
    public FeatureGroupResponse findByIdentifier(
            AuthorizationContext authorizationContext, @PathVariable String featureGroupIdentifier) {
        return FeatureGroupResponse.from(facade.findByIdentifier(authorizationContext, featureGroupIdentifier));
    }

    @GetMapping
    public Page<FeatureGroupResponse> findAll(
            AuthorizationContext authorizationContext,
            @RequestParam(required = false) String lifecycle,
            @RequestParam(required = false) String featureIdentifier,
            @PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return facade.findAll(authorizationContext, featureIdentifier, lifecycle, pageable)
                .map(FeatureGroupResponse::from);
    }

    @PutMapping("/{featureGroupIdentifier}")
    public FeatureGroupResponse update(
            AuthorizationContext authorizationContext,
            @PathVariable String featureGroupIdentifier,
            @RequestBody UpdateFeatureGroupRequest request) {
        return FeatureGroupResponse.from(
                facade.update(authorizationContext, featureGroupIdentifier, request.toInput()));
    }

    @PatchMapping("/{featureGroupIdentifier}/activate")
    public FeatureGroupResponse activate(
            AuthorizationContext authorizationContext, @PathVariable String featureGroupIdentifier) {
        return FeatureGroupResponse.from(facade.activate(authorizationContext, featureGroupIdentifier));
    }

    @PatchMapping("/{featureGroupIdentifier}/inactivate")
    public FeatureGroupResponse inactivate(
            AuthorizationContext authorizationContext, @PathVariable String featureGroupIdentifier) {
        return FeatureGroupResponse.from(facade.inactivate(authorizationContext, featureGroupIdentifier));
    }

    @DeleteMapping("/{featureGroupIdentifier}")
    public ResponseEntity<Void> purge(
            AuthorizationContext authorizationContext, @PathVariable String featureGroupIdentifier) {
        facade.purge(authorizationContext, featureGroupIdentifier);
        return ResponseEntity.noContent().build();
    }
}
