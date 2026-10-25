package br.com.portalmanager.platform.workspace.integration.foundation.schema.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import br.com.portalmanager.platform.workspace.entrypoint.schema.response.SchemaResponse;
import br.com.portalmanager.platform.workspace.entrypoint.schema.response.SchemaVersionResponse;
import io.restassured.response.ValidatableResponse;
import java.util.List;
import org.hamcrest.Matchers;

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
        return extractVersions().getFirst().identifier();
    }

    public SchemaVersionResponse extractVersion() {
        return extract(SchemaVersionResponse.class);
    }

    public List<SchemaVersionResponse> extractVersions() {
        return response.extract().jsonPath().getList("", SchemaVersionResponse.class);
    }

    public SchemaClientResponse expectContainsItem(String path, Object value) {
        response.body(path, Matchers.hasItem(value));
        return this;
    }
}
