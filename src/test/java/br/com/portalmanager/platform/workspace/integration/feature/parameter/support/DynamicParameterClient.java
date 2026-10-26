package br.com.portalmanager.platform.workspace.integration.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.request.CreateParameterKeyRequest;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.request.UpdateParameterKeyRequest;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public final class DynamicParameterClient extends BaseClient {

    private static final String BASE = "/api/v1/dynamic-parameters";

    public DynamicParameterClient(PlatformRequestSpecificationFactory requests) {
        super(requests, JsonMapper.builder().build());
    }

    private io.restassured.specification.RequestSpecification parameterRequest() {
        return authorizedRequest(AuthorizationRequestData.builder()
                .token("dynamic-parameter-runtime-review").build())
                .header("correlation-id", "dynamic-parameter-runtime-review");
    }

    public DynamicParameterResponse create(CreateParameterKeyRequest body) {
        return response(parameterRequest().body(json(body)).post(BASE).then(), DynamicParameterResponse::new);
    }

    public DynamicParameterResponse findByIdentifier(String identifier) {
        return response(parameterRequest().get(BASE + "/{identifier}", identifier).then(), DynamicParameterResponse::new);
    }

    public DynamicParameterResponse findAll(String microserviceIdentifier, String status, String type, String key) {
        var request = parameterRequest();
        if (microserviceIdentifier != null) request.queryParam("microserviceIdentifier", microserviceIdentifier);
        if (status != null) request.queryParam("status", status);
        if (type != null) request.queryParam("type", type);
        if (key != null) request.queryParam("key", key);
        return response(request.get(BASE).then(), DynamicParameterResponse::new);
    }

    public DynamicParameterResponse publish(List<UUID> identifiers) {
        return response(parameterRequest().body(json(new br.com.portalmanager.platform.workspace.entrypoint.parameter.request.PublishParameterKeysRequest(identifiers)))
                .post(BASE + "/publish").then(), DynamicParameterResponse::new);
    }

    DynamicParameterResponse publishBatch(Object identifiers) {
        return response(parameterRequest().body(Map.of("identifiers", identifiers))
                .post(BASE + "/publish").then(), DynamicParameterResponse::new);
    }

    public DynamicParameterResponse update(String identifier, UpdateParameterKeyRequest body) {
        return response(parameterRequest().body(json(body)).put(BASE + "/{identifier}", identifier).then(),
                DynamicParameterResponse::new);
    }

    public DynamicParameterResponse inactivate(String identifier) {
        return response(parameterRequest().patch(BASE + "/{identifier}/inactivate", identifier).then(),
                DynamicParameterResponse::new);
    }

    public DynamicParameterResponse activate(String identifier) {
        return response(parameterRequest().patch(BASE + "/{identifier}/activate", identifier).then(),
                DynamicParameterResponse::new);
    }

    public DynamicParameterResponse delete(String identifier) {
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
