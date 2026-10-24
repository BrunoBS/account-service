package br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle.message;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;

public final class MessageClientResponse extends BaseResponse<MessageClientResponse> {
    MessageClientResponse(ValidatableResponse response) {
        super(response);
    }

    public String identifier() {
        return response.extract().path("identifier");
    }
}
