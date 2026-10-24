package br.com.portalmanager.platform.workspace.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;

final class MicroserviceResponse extends BaseResponse<MicroserviceResponse> {

    MicroserviceResponse(ValidatableResponse response) {
        super(response);
    }

    String identifier() {
        return response.extract().path("identifier");
    }
}
