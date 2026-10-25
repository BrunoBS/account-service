package br.com.portalmanager.platform.workspace.integration.core.environment.type.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;

public final class EnvironmentTypeResponse extends BaseResponse<EnvironmentTypeResponse> {
    public EnvironmentTypeResponse(ValidatableResponse response) { super(response); }
    public int statusCode() { return response.extract().statusCode(); }
    public EnvironmentTypeResponse expectContainsItem(String path, Object value) {
        response.body(path, org.hamcrest.Matchers.hasItem(value));
        return this;
    }
    public String code() { return response.extract().path("code"); }
    public br.com.portalmanager.platform.workspace.entrypoint.environment.response.EnvironmentTypeResponse extractBody() {
        return extract(br.com.portalmanager.platform.workspace.entrypoint.environment.response.EnvironmentTypeResponse.class);
    }
}
