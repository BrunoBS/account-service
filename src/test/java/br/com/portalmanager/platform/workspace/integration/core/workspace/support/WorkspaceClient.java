package br.com.portalmanager.platform.workspace.integration.core.workspace.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.request.CreateWorkspaceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.request.UpdateWorkspaceRequest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class WorkspaceClient extends BaseClient {
    private static final String WORKSPACES = "/api/v1/workspaces";

    public WorkspaceClient(PlatformRequestSpecificationFactory requests) {
        super(requests, JsonMapper.builder().build());
    }

    public WorkspaceResponse create(CreateWorkspaceRequest request) {
        return response(ownerRequest().body(json(request)).post(WORKSPACES).then(), WorkspaceResponse::new);
    }

    public WorkspaceResponse findById(String identifier) {
        return response(ownerRequest().get(WORKSPACES + "/{identifier}", identifier).then(), WorkspaceResponse::new);
    }

    public FindAllAction findAll() {
        return new FindAllAction();
    }

    public WorkspaceResponse update(String identifier, UpdateWorkspaceRequest request) {
        return response(ownerRequest().body(json(request)).put(WORKSPACES + "/{identifier}", identifier).then(),
                WorkspaceResponse::new);
    }

    public WorkspaceResponse inactivate(String identifier) {
        return response(ownerRequest().post(WORKSPACES + "/{identifier}/inactivate", identifier).then(),
                WorkspaceResponse::new);
    }

    public WorkspaceResponse restore(String identifier) {
        return response(ownerRequest().post(WORKSPACES + "/{identifier}/restore", identifier).then(),
                WorkspaceResponse::new);
    }

    public WorkspaceResponse delete(String identifier) {
        return response(ownerRequest().delete(WORKSPACES + "/{identifier}", identifier).then(),
                WorkspaceResponse::new);
    }

    public WorkspaceResponse createProtocolRequest(String rawJson) {
        return response(ownerRequest().body(rawJson).post(WORKSPACES).then(), WorkspaceResponse::new);
    }

    /** Protocol-specific request used to verify malformed JSON handling. */
    public WorkspaceResponse createMalformedJson(String body) {
        return response(ownerRequest().body(body).post(WORKSPACES).then(), WorkspaceResponse::new);
    }

    public WorkspaceResponse updateProtocolRequest(String identifier, String rawJson) {
        return response(ownerRequest().body(rawJson).put(WORKSPACES + "/{identifier}", identifier).then(),
                WorkspaceResponse::new);
    }

    /** Protocol-specific request used to verify that the correlation header is mandatory. */
    public WorkspaceResponse findWithoutCorrelationId() {
        return response(authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .accept(ContentType.JSON).get(WORKSPACES).then(), WorkspaceResponse::new);
    }

    /** Protocol-specific request used to verify that the bearer token is mandatory. */
    public WorkspaceResponse findWithoutBearerToken() {
        return response(request().header("correlation-id", "workspace-it").accept(ContentType.JSON)
                .get(WORKSPACES).then(), WorkspaceResponse::new);
    }

    /** Protocol-specific request used to verify authentication when both required headers are missing. */
    public WorkspaceResponse findWithoutAuthenticationHeaders() {
        return response(request().accept(ContentType.JSON).get(WORKSPACES).then(), WorkspaceResponse::new);
    }

    public final class FindAllAction {
        private final RequestSpecification request = ownerRequest();

        public FindAllAction lifecycle(String value) {
            request.queryParam("lifecycle", value);
            return this;
        }

        public FindAllAction typeName(String value) {
            request.queryParam("typeName", value);
            return this;
        }

        public FindAllAction tagName(String value) {
            request.queryParam("tagName", value);
            return this;
        }

        public FindAllAction page(int value) {
            request.queryParam("page", value);
            return this;
        }

        public FindAllAction page(String value) {
            request.queryParam("page", value);
            return this;
        }

        public FindAllAction size(int value) {
            request.queryParam("size", value);
            return this;
        }

        public FindAllAction size(String value) {
            request.queryParam("size", value);
            return this;
        }

        public FindAllAction sort(String value) {
            request.queryParam("sort", value);
            return this;
        }

        public WorkspaceResponse execute() {
            return response(request.get(WORKSPACES).then(), WorkspaceResponse::new);
        }
    }

    private io.restassured.specification.RequestSpecification ownerRequest() {
        return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "workspace-it")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }
}
