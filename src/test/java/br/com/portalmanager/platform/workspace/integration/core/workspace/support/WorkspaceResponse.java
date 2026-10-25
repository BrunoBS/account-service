package br.com.portalmanager.platform.workspace.integration.core.workspace.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matchers;

public final class WorkspaceResponse extends BaseResponse<WorkspaceResponse> {
    public WorkspaceResponse(ValidatableResponse response) {
        super(response);
    }

    public br.com.portalmanager.platform.workspace.entrypoint.workspace.response.WorkspaceResponse extractBody() {
        return extract(br.com.portalmanager.platform.workspace.entrypoint.workspace.response.WorkspaceResponse.class);
    }

    public WorkspaceResponse expectContainsItem(String path, Object value) {
        response.body(path, Matchers.hasItem(value));
        return this;
    }

    public WorkspaceResponse expectNotContainsItem(String path, Object value) {
        response.body(path, Matchers.not(Matchers.hasItem(value)));
        return this;
    }

    public WorkspaceResponse expectBodyDoesNotContain(String value) {
        response.body(Matchers.not(Matchers.containsString(value)));
        return this;
    }

    public int statusCode() {
        return response.extract().statusCode();
    }

    public <T> java.util.List<T> extractList(String path, Class<T> type) {
        return response.extract().jsonPath().getList(path, type);
    }
}
