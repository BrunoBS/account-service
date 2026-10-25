package br.com.portalmanager.platform.workspace.integration.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import io.restassured.response.ValidatableResponse;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/** Prepares the parent resource for dynamic parameter scenarios. */
public final class MicroserviceClient extends BaseClient {

    public MicroserviceClient(PlatformRequestSpecificationFactory requests, JsonMapper jsonMapper) {
        super(requests, jsonMapper);
    }

    public MicroserviceResponse createParent() {
        return create("account-service", "Account");
    }

    public MicroserviceResponse create(String code, String name) {
        ValidatableResponse created = authorizedRequest(AuthorizationRequestData.builder()
                .token("dynamic-parameter-runtime-review").build())
                .header("correlation-id", "dynamic-parameter-runtime-review")
                .body(Map.of("code", code, "name", name, "settings", Map.of()))
                .post("/api/v1/service-contexts/microservices").then();
        return response(created, MicroserviceResponse::new);
    }
}
