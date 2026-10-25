package br.com.portalmanager.platform.workspace.integration.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;

final class DynamicParameterClient extends BaseClient {

    private static final String BASE = "/api/v1/dynamic-parameters";

    DynamicParameterClient(PlatformRequestSpecificationFactory requests, JsonMapper jsonMapper) {
        super(requests, jsonMapper);
    }

    private io.restassured.specification.RequestSpecification parameterRequest() {
        return authorizedRequest(AuthorizationRequestData.builder()
                .token("dynamic-parameter-runtime-review").build())
                .header("correlation-id", "dynamic-parameter-runtime-review");
    }

    DynamicParameterResponse create(Object body) {
        return response(parameterRequest().body(body).post(BASE).then(), DynamicParameterResponse::new);
    }

    DynamicParameterResponse publish(String identifier) {
        return publishBatch(List.of(identifier));
    }

    DynamicParameterResponse publishBatch(Object identifiers) {
        return response(parameterRequest().body(Map.of("identifiers", identifiers))
                .post(BASE + "/publish").then(), DynamicParameterResponse::new);
    }

    DynamicParameterResponse update(String identifier, Object body) {
        return response(parameterRequest().body(body).put(BASE + "/{identifier}", identifier).then(),
                DynamicParameterResponse::new);
    }

    DynamicParameterResponse inactivate(String identifier) {
        return response(parameterRequest().patch(BASE + "/{identifier}/inactivate", identifier).then(),
                DynamicParameterResponse::new);
    }

    DynamicParameterResponse activate(String identifier) {
        return response(parameterRequest().patch(BASE + "/{identifier}/activate", identifier).then(),
                DynamicParameterResponse::new);
    }

    DynamicParameterResponse delete(String identifier) {
        return response(parameterRequest().delete(BASE + "/{identifier}", identifier).then(),
                DynamicParameterResponse::new);
    }

    DynamicParameterResponse publishLegacy(String identifier) {
        return response(parameterRequest().patch(BASE + "/{identifier}/publish", identifier).then(),
                DynamicParameterResponse::new);
    }

    DynamicParameterResponse publishLegacyBatch(Object identifiers) {
        return response(parameterRequest().body(Map.of("identifiers", identifiers))
                .post(BASE + "/publish-batch").then(), DynamicParameterResponse::new);
    }
}
