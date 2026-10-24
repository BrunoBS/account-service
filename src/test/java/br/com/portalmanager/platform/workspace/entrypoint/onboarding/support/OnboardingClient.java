package br.com.portalmanager.platform.workspace.entrypoint.onboarding.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import io.restassured.http.ContentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class OnboardingClient extends BaseClient {
    public OnboardingClient(PlatformRequestSpecificationFactory requests) {
        super(requests, JsonMapper.builder().build());
    }

    public OnboardingClientResponse findProgress(String workspaceIdentifier) {
        return response(authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "workspace-it").accept(ContentType.JSON)
                .get("/api/v1/workspaces/{workspaceIdentifier}/onboarding", workspaceIdentifier).then(),
                OnboardingClientResponse::new);
    }
}
