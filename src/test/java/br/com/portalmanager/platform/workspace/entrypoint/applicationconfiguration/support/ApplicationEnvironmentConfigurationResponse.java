package br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;

public final class ApplicationEnvironmentConfigurationResponse extends BaseResponse<ApplicationEnvironmentConfigurationResponse> {
    public ApplicationEnvironmentConfigurationResponse(ValidatableResponse response) { super(response); }
    public String identifier() { return response.extract().path("identifier"); }
    public ApplicationEnvironmentConfigurationResponse expectContainsItem(String path, Object value) {
        response.body(path, org.hamcrest.Matchers.hasItem(value));
        return this;
    }
    public br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.response.ApplicationEnvironmentConfigurationResponse extractBody() {
        return extract(br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.response.ApplicationEnvironmentConfigurationResponse.class);
    }
}
