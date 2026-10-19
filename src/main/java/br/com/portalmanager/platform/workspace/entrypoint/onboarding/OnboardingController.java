package br.com.portalmanager.platform.workspace.entrypoint.onboarding;

import br.com.portalmanager.platform.library.authorization.model.AuthorizationContext;
import br.com.portalmanager.platform.workspace.entrypoint.onboarding.response.OnboardingProgressResponse;
import br.com.portalmanager.platform.workspace.feature.onboarding.facade.OnboardingFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/workspaces/{workspaceIdentifier}/onboarding")
public class OnboardingController {

    private final OnboardingFacade facade;

    public OnboardingController(OnboardingFacade facade) {
        this.facade = facade;
    }

    @GetMapping
    public OnboardingProgressResponse findProgress(
        AuthorizationContext context,
        @PathVariable String workspaceIdentifier
    ) {
        return OnboardingProgressResponse.from(facade.findProgress(context, workspaceIdentifier));
    }

    @PatchMapping
    public ResponseEntity<Void> complete(AuthorizationContext context, @PathVariable String workspaceIdentifier) {
        facade.complete(context, workspaceIdentifier);
        return ResponseEntity.noContent().build();
    }
}
