package br.com.portalmanager.platform.workspace.entrypoint.web.platform.featurecontext;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.web.platform.feature.response.FeatureResponse;
import br.com.portalmanager.platform.workspace.feature.platform.facade.featurecontext.FeatureContextFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/platform")
public class FeatureContextController {
    private final FeatureContextFacade facade;

    public FeatureContextController(FeatureContextFacade facade) {
        this.facade = facade;
    }

    @PostMapping("/contexts/{contextIdentifier}/features/{featureIdentifier}")
    public FeatureResponse associate(AuthorizationContext authorizationContext, @PathVariable String contextIdentifier,
        @PathVariable String featureIdentifier) {
        return FeatureResponse.from(facade.associate(authorizationContext, contextIdentifier, featureIdentifier));
    }

    @DeleteMapping("/contexts/{contextIdentifier}/features/{featureIdentifier}")
    public ResponseEntity<Void> remove(AuthorizationContext authorizationContext, @PathVariable String contextIdentifier,
        @PathVariable String featureIdentifier) {
        facade.remove(authorizationContext, contextIdentifier, featureIdentifier);
        return ResponseEntity.noContent().build();
    }
}
