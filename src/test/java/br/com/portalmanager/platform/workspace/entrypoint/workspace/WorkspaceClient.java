package br.com.portalmanager.platform.workspace.entrypoint.workspace;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

/** HTTP operations used by the workspace controller scenarios. */
final class WorkspaceClient extends BaseClient {

    private static final String WORKSPACES = "/api/v1/workspaces";
    private final String token;
    private final String correlationId;

    WorkspaceClient(PlatformRequestSpecificationFactory requests, JsonMapper jsonMapper) {
        this(requests, jsonMapper, "workspace-api-it", "workspace-api-it");
    }

    WorkspaceClient(PlatformRequestSpecificationFactory requests, JsonMapper jsonMapper,
                    String token, String correlationId) {
        super(requests, jsonMapper);
        this.token = token;
        this.correlationId = correlationId;
    }

    private RequestSpecification ownerRequest() {
        return authorizedRequest(AuthorizationRequestData.builder().token(token).build())
                .header("correlation-id", correlationId)
                .accept(ContentType.JSON);
    }

    WorkspaceResponse find(String path) {
        return response(ownerRequest().get(path).then(), WorkspaceResponse::new);
    }

    WorkspaceResponse create(Map<String, Object> body) {
        return response(ownerRequest().body(body).post(WORKSPACES).then(), WorkspaceResponse::new);
    }

    WorkspaceResponse action(String path) {
        return response(ownerRequest().post(path).then(), WorkspaceResponse::new);
    }

    WorkspaceResponse update(String path, Map<String, Object> body) {
        return response(ownerRequest().body(body).put(path).then(), WorkspaceResponse::new);
    }

    WorkspaceResponse updateByIdentifier(String identifier, Map<String, Object> body) {
        return update(WORKSPACES + "/" + identifier, body);
    }

    WorkspaceResponse delete(String path) {
        return response(ownerRequest().delete(path).then(), WorkspaceResponse::new);
    }

    WorkspaceResponse deleteByIdentifier(String identifier) {
        return delete(WORKSPACES + "/" + identifier);
    }

    WorkspaceResponse listActiveByType(String typeName) {
        return response(ownerRequest()
                .queryParam("lifecycle", "ACTIVE")
                .queryParam("typeName", typeName)
                .get(WORKSPACES).then(), WorkspaceResponse::new);
    }

    WorkspaceResponse findByTag(String tagName) {
        return response(ownerRequest().queryParam("tagName", tagName)
                .get(WORKSPACES).then(), WorkspaceResponse::new);
    }

    /** For tests whose subject is the exact HTTP request shape. */
    RequestSpecification protocolRequest() {
        return ownerRequest();
    }
}
