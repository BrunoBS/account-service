package br.com.portalmanager.platform.workspace.entrypoint.schema;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import br.com.portalmanager.platform.workspace.entrypoint.schema.response.SchemaResponse;
import io.restassured.response.ValidatableResponse;
import java.util.List;

public final class SchemaClientResponse extends BaseResponse<SchemaClientResponse> {
    public SchemaClientResponse(ValidatableResponse response) {
        super(response);
    }

    public SchemaResponse extractSchema() {
        return extract(SchemaResponse.class);
    }

    public List<SchemaResponse> extractSchemas() {
        return response.extract().jsonPath().getList("", SchemaResponse.class);
    }

    public String extractVersionIdentifier() {
        return response.extract().jsonPath().getString("[0].identifier");
    }
}
