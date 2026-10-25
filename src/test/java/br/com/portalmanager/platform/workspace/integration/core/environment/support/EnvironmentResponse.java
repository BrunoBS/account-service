package br.com.portalmanager.platform.workspace.integration.core.environment.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;

public final class EnvironmentResponse extends BaseResponse<EnvironmentResponse> {
    public EnvironmentResponse(ValidatableResponse response) { super(response); }
    public String identifier() { return response.extract().path("identifier"); }
    public EnvironmentResponse expectContainsItem(String path, Object value) {
        response.body(path, org.hamcrest.Matchers.hasItem(value));
        return this;
    }
    public br.com.portalmanager.platform.workspace.entrypoint.environment.response.EnvironmentResponse extractBody() {
        return extract(br.com.portalmanager.platform.workspace.entrypoint.environment.response.EnvironmentResponse.class);
    }
}
