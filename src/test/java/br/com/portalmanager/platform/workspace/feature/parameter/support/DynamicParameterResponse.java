package br.com.portalmanager.platform.workspace.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;

final class DynamicParameterResponse extends BaseResponse<DynamicParameterResponse> {

    DynamicParameterResponse(ValidatableResponse response) {
        super(response);
    }

    String identifier() {
        return response.extract().path("identifier");
    }
}
