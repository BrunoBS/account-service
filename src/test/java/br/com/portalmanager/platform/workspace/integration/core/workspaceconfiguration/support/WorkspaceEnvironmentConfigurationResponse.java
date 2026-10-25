package br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;

public final class WorkspaceEnvironmentConfigurationResponse
        extends BaseResponse<WorkspaceEnvironmentConfigurationResponse> {
    public WorkspaceEnvironmentConfigurationResponse(ValidatableResponse response) {
        super(response);
    }

    public String identifier() {
        return response.extract().path("identifier");
    }

    public WorkspaceEnvironmentConfigurationResponse expectContainsItem(String path, Object value) {
        response.body(path, org.hamcrest.Matchers.hasItem(value));
        return this;
    }

    public br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.response.WorkspaceEnvironmentConfigurationResponse extractBody() {
        return extract(br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.response.WorkspaceEnvironmentConfigurationResponse.class);
    }
}
