package br.com.portalmanager.platform.workspace.entrypoint.schema;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaBindingRequest;
import io.restassured.http.ContentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class SchemaBindingClient extends BaseClient {
    public SchemaBindingClient(PlatformRequestSpecificationFactory requests) {
        super(requests, JsonMapper.builder().build());
    }

    public SchemaBindingClientResponse create(CreateSchemaBindingRequest request) {
        return response(ownerRequest().body(json(request)).post("/api/v1/schema-bindings").then(),
                SchemaBindingClientResponse::new);
    }

    public SchemaBindingClientResponse findAll() {
        return response(ownerRequest().get("/api/v1/schema-bindings").then(), SchemaBindingClientResponse::new);
    }

    private io.restassured.specification.RequestSpecification ownerRequest() {
        return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "schema-it")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

}
