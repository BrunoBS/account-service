package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message.request.CreateMessageRequest;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message.request.UpdateMessageRequest;
import java.util.Map;
import io.restassured.http.ContentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class MessageClient extends BaseClient {
    public MessageClient(PlatformRequestSpecificationFactory requests) {
        super(requests, JsonMapper.builder().build());
    }

    public MessageClientResponse create(CreateMessageRequest request) {
        var response = ownerRequest().body(json(request)).post("/api/v1/dynamic-bundles/messages").then();
        return response(response, MessageClientResponse::new);
    }

    public MessageClientResponse findByIdentifier(String identifier) {
        var result = ownerRequest().get("/api/v1/dynamic-bundles/messages/{identifier}", identifier).then();
        return response(result, MessageClientResponse::new);
    }

    public MessageClientResponse findAll(Map<String, Object> filters) {
        var result = ownerRequest().queryParams(filters).get("/api/v1/dynamic-bundles/messages").then();
        return response(result, MessageClientResponse::new);
    }

    public MessageClientResponse update(String identifier, UpdateMessageRequest request) {
        var result = ownerRequest().body(json(request))
                .put("/api/v1/dynamic-bundles/messages/{identifier}", identifier).then();
        return response(result, MessageClientResponse::new);
    }

    public MessageClientResponse activate(String identifier) {
        return response(ownerRequest().patch("/api/v1/dynamic-bundles/messages/{identifier}/activate", identifier).then(),
                MessageClientResponse::new);
    }

    public MessageClientResponse inactivate(String identifier) {
        return response(ownerRequest().patch("/api/v1/dynamic-bundles/messages/{identifier}/inactivate", identifier).then(),
                MessageClientResponse::new);
    }

    public MessageClientResponse delete(String identifier) {
        return response(ownerRequest().delete("/api/v1/dynamic-bundles/messages/{identifier}", identifier).then(),
                MessageClientResponse::new);
    }

    private io.restassured.specification.RequestSpecification ownerRequest() {
        return authorizedRequest(AuthorizationRequestData.builder().token("message-setup-it").build())
                .header("correlation-id", "message-setup-it")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }
}
