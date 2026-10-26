package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.messagetranslator.request.CreateMessageTranslatorRequest;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.messagetranslator.request.UpdateMessageTranslatorRequest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class MessageTranslatorClient extends BaseClient {
    public MessageTranslatorClient(PlatformRequestSpecificationFactory requests) {
        super(requests, JsonMapper.builder().build());
    }

    public MessageTranslatorClientResponse create(String messageIdentifier, CreateMessageTranslatorRequest request) {
        return response(owner().body(json(request)).post(path(messageIdentifier)).then(), MessageTranslatorClientResponse::new);
    }
    public MessageTranslatorClientResponse findByIdentifier(String messageIdentifier, String translatorIdentifier) {
        return response(owner().get(path(messageIdentifier) + "/{translatorIdentifier}", translatorIdentifier).then(), MessageTranslatorClientResponse::new);
    }
    public MessageTranslatorClientResponse findAll(String messageIdentifier, Map<String, Object> filters) {
        return response(owner().queryParams(filters).get(path(messageIdentifier)).then(), MessageTranslatorClientResponse::new);
    }
    public MessageTranslatorClientResponse update(String messageIdentifier, String translatorIdentifier, UpdateMessageTranslatorRequest request) {
        return response(owner().body(json(request)).put(path(messageIdentifier) + "/{translatorIdentifier}", translatorIdentifier).then(), MessageTranslatorClientResponse::new);
    }
    public MessageTranslatorClientResponse activate(String messageIdentifier, String translatorIdentifier) {
        return response(owner().patch(path(messageIdentifier) + "/{translatorIdentifier}/activate", translatorIdentifier).then(), MessageTranslatorClientResponse::new);
    }
    public MessageTranslatorClientResponse inactivate(String messageIdentifier, String translatorIdentifier) {
        return response(owner().patch(path(messageIdentifier) + "/{translatorIdentifier}/inactivate", translatorIdentifier).then(), MessageTranslatorClientResponse::new);
    }
    public MessageTranslatorClientResponse delete(String messageIdentifier, String translatorIdentifier) {
        return response(owner().delete(path(messageIdentifier) + "/{translatorIdentifier}", translatorIdentifier).then(), MessageTranslatorClientResponse::new);
    }
    private String path(String messageIdentifier) {
        return "/api/v1/dynamic-bundles/messages/" + messageIdentifier + "/translators";
    }
    private io.restassured.specification.RequestSpecification owner() {
        return authorizedRequest(AuthorizationRequestData.builder().token("message-it").build())
                .header("correlation-id", "message-it").contentType(ContentType.JSON).accept(ContentType.JSON);
    }
}
