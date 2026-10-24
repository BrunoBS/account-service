package br.com.portalmanager.platform.workspace.entrypoint.workspace;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import io.restassured.response.ValidatableResponse;

final class WorkspaceResponse extends BaseResponse<WorkspaceResponse> {

    WorkspaceResponse(ValidatableResponse response) {
        super(response);
    }

    String identifier() {
        return response.extract().path("identifier");
    }

    Integer version() {
        return response.extract().path("version");
    }
}
