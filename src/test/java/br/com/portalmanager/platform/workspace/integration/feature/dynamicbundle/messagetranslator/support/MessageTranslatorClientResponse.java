package br.com.portalmanager.platform.workspace.integration.feature.dynamicbundle.messagetranslator.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.messagetranslator.response.MessageTranslatorResponse;
import io.restassured.response.ValidatableResponse;

public final class MessageTranslatorClientResponse extends BaseResponse<MessageTranslatorClientResponse> {
    public MessageTranslatorClientResponse(ValidatableResponse response) { super(response); }
    public String identifier() { return response.extract().path("identifier"); }
    public MessageTranslatorResponse extractBody() { return extract(MessageTranslatorResponse.class); }
}
