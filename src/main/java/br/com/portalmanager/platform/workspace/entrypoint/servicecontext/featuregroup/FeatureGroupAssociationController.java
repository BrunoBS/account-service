package br.com.portalmanager.platform.workspace.entrypoint.servicecontext.featuregroup;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.feature.response.FeatureResponse;
import br.com.portalmanager.platform.workspace.feature.servicecontext.facade.featuregroup.FeatureGroupAssociationFacade;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/service-contexts")
public class FeatureGroupAssociationController {
    private final FeatureGroupAssociationFacade facade;

    public FeatureGroupAssociationController(FeatureGroupAssociationFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/feature-groups/{featureGroupIdentifier}/features/{featureIdentifier}")
    public FeatureResponse associate(
            AuthorizationContext authorizationContext,
            @PathVariable String featureGroupIdentifier,
            @PathVariable String featureIdentifier) {
        return FeatureResponse.from(
                facade.associate(authorizationContext, featureGroupIdentifier, featureIdentifier));
    }

    @DeleteMapping("/feature-groups/{featureGroupIdentifier}/features/{featureIdentifier}")
    public ResponseEntity<Void> remove(
            AuthorizationContext authorizationContext,
            @PathVariable String featureGroupIdentifier,
            @PathVariable String featureIdentifier) {
        facade.remove(authorizationContext, featureGroupIdentifier, featureIdentifier);
        return ResponseEntity.noContent().build();
    }
}
