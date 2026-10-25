package br.com.portalmanager.platform.workspace.integration.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;

public final class MicroserviceResponse extends BaseResponse<MicroserviceResponse> {

    MicroserviceResponse(ValidatableResponse response) {
        super(response);
    }

    public String identifier() {
        return response.extract().path("identifier");
    }
}
