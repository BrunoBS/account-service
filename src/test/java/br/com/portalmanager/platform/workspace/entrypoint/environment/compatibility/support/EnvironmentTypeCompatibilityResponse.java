package br.com.portalmanager.platform.workspace.entrypoint.environment.compatibility.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;

public final class EnvironmentTypeCompatibilityResponse extends BaseResponse<EnvironmentTypeCompatibilityResponse> {
    public EnvironmentTypeCompatibilityResponse(ValidatableResponse response) { super(response); }
    public int statusCode() { return response.extract().statusCode(); }
    public String identifier() { return response.extract().path("identifier"); }
    public EnvironmentTypeCompatibilityResponse expectContainsItem(String path, Object value) {
        response.body(path, org.hamcrest.Matchers.hasItem(value));
        return this;
    }
    public <T> java.util.List<T> extractList(Class<T> type) { return response.extract().jsonPath().getList("", type); }
}
