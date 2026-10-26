package br.com.portalmanager.platform.workspace.integration.feature.parameter.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.response.ParameterKeyMetadataResponse;
import br.com.portalmanager.platform.workspace.entrypoint.parameter.response.ParameterKeyResponse;
import io.restassured.response.ValidatableResponse;

public final class DynamicParameterResponse extends BaseResponse<DynamicParameterResponse> {

    DynamicParameterResponse(ValidatableResponse response) {
        super(response);
    }

    public String identifier() {
        return response.extract().path("identifier");
    }

    public ParameterKeyMetadataResponse extractMetadata() { return extract(ParameterKeyMetadataResponse.class); }
    public ParameterKeyResponse extractBody() { return extract(ParameterKeyResponse.class); }
    public String status() { return response.extract().path("status"); }

    public DynamicParameterResponse expectAccepted() {
        response.statusCode(202);
        return this;
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
