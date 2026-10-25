package br.com.portalmanager.platform.workspace.entrypoint.schema;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import br.com.portalmanager.platform.workspace.entrypoint.schema.response.SchemaBindingResponse;
import io.restassured.response.ValidatableResponse;
import java.util.List;

public final class SchemaBindingClientResponse extends BaseResponse<SchemaBindingClientResponse> {
    public SchemaBindingClientResponse(ValidatableResponse response) {
        super(response);
    }

    public SchemaBindingResponse extractBinding() {
        return extract(SchemaBindingResponse.class);
    }

    public List<SchemaBindingResponse> extractBindings() {
        return response.extract().jsonPath().getList("", SchemaBindingResponse.class);
    }
}
