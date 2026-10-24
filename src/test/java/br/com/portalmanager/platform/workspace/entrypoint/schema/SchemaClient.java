package br.com.portalmanager.platform.workspace.entrypoint.schema;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaRequest;
import io.restassured.http.ContentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class SchemaClient extends BaseClient {
    private static final String SCHEMAS = "/api/v1/schemas";

    public SchemaClient(PlatformRequestSpecificationFactory requests) {
        super(requests, JsonMapper.builder().build());
    }

    public SchemaClientResponse create(CreateSchemaRequest request) {
        return response(ownerRequest().body(json(request)).post(SCHEMAS).then(), SchemaClientResponse::new);
    }

    public SchemaClientResponse findVersions(String schemaIdentifier) {
        return response(ownerRequest().get(SCHEMAS + "/{identifier}/versions", schemaIdentifier).then(),
                SchemaClientResponse::new);
    }

    public SchemaClientResponse publish(String schemaIdentifier, String versionIdentifier) {
        return response(ownerRequest().patch(SCHEMAS + "/{identifier}/versions/{versionIdentifier}/publish",
                schemaIdentifier, versionIdentifier).then(), SchemaClientResponse::new);
    }

    private io.restassured.specification.RequestSpecification ownerRequest() {
        return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "schema-it")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }
}
