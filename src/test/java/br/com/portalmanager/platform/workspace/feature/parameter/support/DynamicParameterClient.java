package br.com.portalmanager.platform.workspace.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

final class DynamicParameterClient extends BaseClient {

    private static final String BASE = "/api/v1/dynamic-parameters";

    DynamicParameterClient(PlatformRequestSpecificationFactory requests, JsonMapper jsonMapper) {
        super(requests, jsonMapper);
    }

    RequestSpecification protocolRequest() {
        return authorizedRequest(AuthorizationRequestData.builder()
                .token("dynamic-parameter-runtime-review").build())
                .header("correlation-id", "dynamic-parameter-runtime-review")
                .accept(ContentType.JSON);
    }

    DynamicParameterResponse createRaw(String body) {
        return response(protocolRequest().body(body).post(BASE).then(), DynamicParameterResponse::new);
    }

    DynamicParameterResponse publish(String identifier) {
        return response(protocolRequest().body(Map.of("identifiers", java.util.List.of(identifier)))
                .post(BASE + "/publish").then(), DynamicParameterResponse::new);
    }
}
