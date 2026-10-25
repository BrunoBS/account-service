package br.com.portalmanager.platform.workspace.integration.foundation.schema.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaBindingRequest;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.UpdateSchemaBindingRequest;
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

    public SchemaBindingClientResponse findAll(String lifecycle) {
        return response(ownerRequest().queryParam("lifecycle", lifecycle).get("/api/v1/schema-bindings").then(),
                SchemaBindingClientResponse::new);
    }

    public SchemaBindingClientResponse find(String identifier) {
        return response(ownerRequest().get("/api/v1/schema-bindings/{identifier}", identifier).then(),
                SchemaBindingClientResponse::new);
    }

    public SchemaBindingClientResponse update(String identifier, UpdateSchemaBindingRequest request) {
        return response(ownerRequest().body(json(request)).put("/api/v1/schema-bindings/{identifier}", identifier).then(),
                SchemaBindingClientResponse::new);
    }

    public SchemaBindingClientResponse activate(String identifier) {
        return response(ownerRequest().patch("/api/v1/schema-bindings/{identifier}/activate", identifier).then(),
                SchemaBindingClientResponse::new);
    }

    public SchemaBindingClientResponse inactivate(String identifier) {
        return response(ownerRequest().patch("/api/v1/schema-bindings/{identifier}/inactivate", identifier).then(),
                SchemaBindingClientResponse::new);
    }

    public SchemaBindingClientResponse delete(String identifier) {
        return response(ownerRequest().delete("/api/v1/schema-bindings/{identifier}", identifier).then(),
                SchemaBindingClientResponse::new);
    }

    private io.restassured.specification.RequestSpecification ownerRequest() {
        return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "schema-it")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

}
