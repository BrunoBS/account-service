package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.message.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message.request.CreateMessageRequest;
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

    private io.restassured.specification.RequestSpecification ownerRequest() {
        return authorizedRequest(AuthorizationRequestData.builder().token("message-setup-it").build())
                .header("correlation-id", "message-setup-it")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }
}
