package br.com.portalmanager.platform.workspace.integration.foundation.schema.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaRequest;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.CreateSchemaVersionRequest;
import br.com.portalmanager.platform.workspace.entrypoint.schema.request.UpdateSchemaRequest;
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

    public SchemaClientResponse create(String workspaceIdentifier, CreateSchemaRequest request) {
        return response(ownerRequest().body(json(request))
                .post("/api/v1/workspaces/{workspaceIdentifier}/schemas", workspaceIdentifier).then(),
                SchemaClientResponse::new);
    }

    public SchemaClientResponse findAll() {
        return response(ownerRequest().get(SCHEMAS).then(), SchemaClientResponse::new);
    }

    public SchemaClientResponse findAll(String lifecycle) {
        return response(ownerRequest().queryParam("lifecycle", lifecycle).get(SCHEMAS).then(), SchemaClientResponse::new);
    }

    public SchemaClientResponse findAll(String workspaceIdentifier, String lifecycle) {
        return response(ownerRequest().queryParam("lifecycle", lifecycle)
                .get("/api/v1/workspaces/{workspaceIdentifier}/schemas", workspaceIdentifier).then(),
                SchemaClientResponse::new);
    }

    public SchemaClientResponse findByIdentifier(String identifier) {
        return response(ownerRequest().get(SCHEMAS + "/{identifier}", identifier).then(), SchemaClientResponse::new);
    }

    public SchemaClientResponse findByIdentifier(String workspaceIdentifier, String identifier) {
        return response(ownerRequest().get("/api/v1/workspaces/{workspaceIdentifier}/schemas/{identifier}",
                workspaceIdentifier, identifier).then(), SchemaClientResponse::new);
    }

    public SchemaClientResponse update(String identifier, UpdateSchemaRequest request) {
        return response(ownerRequest().body(json(request)).put(SCHEMAS + "/{identifier}", identifier).then(),
                SchemaClientResponse::new);
    }

    public SchemaClientResponse update(String workspaceIdentifier, String identifier, UpdateSchemaRequest request) {
        return response(ownerRequest().body(json(request)).put(
                "/api/v1/workspaces/{workspaceIdentifier}/schemas/{identifier}", workspaceIdentifier, identifier).then(),
                SchemaClientResponse::new);
    }

    public SchemaClientResponse activate(String identifier) {
        return response(ownerRequest().patch(SCHEMAS + "/{identifier}/activate", identifier).then(),
                SchemaClientResponse::new);
    }

    public SchemaClientResponse activate(String workspaceIdentifier, String identifier) {
        return response(ownerRequest().patch(
                "/api/v1/workspaces/{workspaceIdentifier}/schemas/{identifier}/activate", workspaceIdentifier,
                identifier).then(), SchemaClientResponse::new);
    }

    public SchemaClientResponse inactivate(String identifier) {
        return response(ownerRequest().patch(SCHEMAS + "/{identifier}/inactivate", identifier).then(),
                SchemaClientResponse::new);
    }

    public SchemaClientResponse inactivate(String workspaceIdentifier, String identifier) {
        return response(ownerRequest().patch(
                "/api/v1/workspaces/{workspaceIdentifier}/schemas/{identifier}/inactivate", workspaceIdentifier,
                identifier).then(), SchemaClientResponse::new);
    }

    public SchemaClientResponse quarantine(String identifier) {
        return response(ownerRequest().delete(SCHEMAS + "/{identifier}", identifier).then(),
                SchemaClientResponse::new);
    }

    public SchemaClientResponse quarantine(String workspaceIdentifier, String identifier) {
        return response(ownerRequest().delete(
                "/api/v1/workspaces/{workspaceIdentifier}/schemas/{identifier}", workspaceIdentifier, identifier).then(),
                SchemaClientResponse::new);
    }

    public SchemaClientResponse createVersion(String identifier, CreateSchemaVersionRequest request) {
        return response(ownerRequest().body(json(request)).post(SCHEMAS + "/{identifier}/versions", identifier).then(),
                SchemaClientResponse::new);
    }

    public SchemaClientResponse createVersion(String workspaceIdentifier, String identifier,
            CreateSchemaVersionRequest request) {
        return response(ownerRequest().body(json(request)).post(
                "/api/v1/workspaces/{workspaceIdentifier}/schemas/{identifier}/versions", workspaceIdentifier,
                identifier).then(), SchemaClientResponse::new);
    }

    public SchemaClientResponse deleteDraft(String identifier, String versionIdentifier) {
        return response(ownerRequest().delete(SCHEMAS + "/{identifier}/versions/{versionIdentifier}", identifier,
                versionIdentifier).then(), SchemaClientResponse::new);
    }

    public SchemaClientResponse deleteDraft(String workspaceIdentifier, String identifier, String versionIdentifier) {
        return response(ownerRequest().delete(
                "/api/v1/workspaces/{workspaceIdentifier}/schemas/{identifier}/versions/{versionIdentifier}",
                workspaceIdentifier, identifier, versionIdentifier).then(), SchemaClientResponse::new);
    }

    public SchemaClientResponse findVersions(String schemaIdentifier) {
        return response(ownerRequest().get(SCHEMAS + "/{identifier}/versions", schemaIdentifier).then(),
                SchemaClientResponse::new);
    }

    public SchemaClientResponse findVersions(String workspaceIdentifier, String schemaIdentifier) {
        return response(ownerRequest().get(
                "/api/v1/workspaces/{workspaceIdentifier}/schemas/{identifier}/versions", workspaceIdentifier,
                schemaIdentifier).then(), SchemaClientResponse::new);
    }

    public SchemaClientResponse publish(String schemaIdentifier, String versionIdentifier) {
        return response(ownerRequest().patch(SCHEMAS + "/{identifier}/versions/{versionIdentifier}/publish",
                schemaIdentifier, versionIdentifier).then(), SchemaClientResponse::new);
    }

    public SchemaClientResponse publish(String workspaceIdentifier, String schemaIdentifier, String versionIdentifier) {
        return response(ownerRequest().patch(
                "/api/v1/workspaces/{workspaceIdentifier}/schemas/{identifier}/versions/{versionIdentifier}/publish",
                workspaceIdentifier, schemaIdentifier, versionIdentifier).then(), SchemaClientResponse::new);
    }

    private io.restassured.specification.RequestSpecification ownerRequest() {
        return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "schema-it")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }
}
