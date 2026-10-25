package br.com.portalmanager.platform.workspace.integration.core.application.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matchers;

public final class ApplicationClientResponse extends BaseResponse<ApplicationClientResponse> {
    public ApplicationClientResponse(ValidatableResponse response) {
        super(response);
    }

    public int statusCode() {
        return response.extract().statusCode();
    }

    public String identifier() {
        return response.extract().path("identifier");
    }

    public br.com.portalmanager.platform.workspace.entrypoint.application.response.ApplicationResponse extractBody() {
        return extract(br.com.portalmanager.platform.workspace.entrypoint.application.response.ApplicationResponse.class);
    }

    public ApplicationClientResponse expectContainsItem(String path, Object value) {
        response.body(path, Matchers.hasItem(value));
        return this;
    }

    public ApplicationClientResponse expectNotContainsItem(String path, Object value) {
        response.body(path, Matchers.not(Matchers.hasItem(value)));
        return this;
    }

    public <T> java.util.List<T> extractList(String path, Class<T> type) {
        return response.extract().jsonPath().getList(path, type);
    }
}
