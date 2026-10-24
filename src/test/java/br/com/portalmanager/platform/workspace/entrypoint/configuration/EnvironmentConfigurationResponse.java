package br.com.portalmanager.platform.workspace.entrypoint.configuration;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;

final class EnvironmentConfigurationResponse extends BaseResponse<EnvironmentConfigurationResponse> {

    EnvironmentConfigurationResponse(ValidatableResponse response) {
        super(response);
    }

    String identifier() {
        return response.extract().path("identifier");
    }
}
