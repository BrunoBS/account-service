package br.com.portalmanager.platform.workspace.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;

public final class DynamicParameterResponse extends BaseResponse<DynamicParameterResponse> {

    DynamicParameterResponse(ValidatableResponse response) {
        super(response);
    }

    String identifier() {
        return response.extract().path("identifier");
    }

    public DynamicParameterResponse expectAcceptedWithEmptyBody() {
        response.statusCode(202).body(org.hamcrest.Matchers.emptyString());
        return this;
    }

    public DynamicParameterResponse expectMethodNotAllowed() {
        response.statusCode(405);
        return this;
    }
}
