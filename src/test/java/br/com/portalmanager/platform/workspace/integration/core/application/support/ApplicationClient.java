package br.com.portalmanager.platform.workspace.integration.core.application.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.application.request.CreateApplicationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.application.request.UpdateApplicationRequest;
import io.restassured.http.ContentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class ApplicationClient extends BaseClient {
    private static final String WORKSPACE_APPLICATIONS = "/api/v1/workspaces/{workspaceIdentifier}/applications";

    public ApplicationClient(PlatformRequestSpecificationFactory requests) {
        super(requests, JsonMapper.builder().build());
    }

    public ApplicationClientResponse create(String workspaceIdentifier, CreateApplicationRequest request) {
        return response(authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "workspace-it")
                .contentType(ContentType.JSON).accept(ContentType.JSON)
                .body(json(request)).post(WORKSPACE_APPLICATIONS, workspaceIdentifier).then(),
                ApplicationClientResponse::new);
    }

    public ApplicationClientResponse findByIdentifier(String workspaceIdentifier, String identifier) {
        return response(ownerRequest().get(WORKSPACE_APPLICATIONS + "/{identifier}", workspaceIdentifier, identifier).then(),
                ApplicationClientResponse::new);
    }

    public FindAllAction findAll(String workspaceIdentifier) { return new FindAllAction(workspaceIdentifier); }

    public ApplicationClientResponse summary(String workspaceIdentifier) {
        return response(ownerRequest().get(WORKSPACE_APPLICATIONS + "/summary", workspaceIdentifier).then(),
                ApplicationClientResponse::new);
    }

    public ApplicationClientResponse update(String workspaceIdentifier, String identifier, UpdateApplicationRequest request) {
        return response(ownerRequest().body(json(request))
                .put(WORKSPACE_APPLICATIONS + "/{identifier}", workspaceIdentifier, identifier).then(),
                ApplicationClientResponse::new);
    }

    public ApplicationClientResponse restore(String workspaceIdentifier, String identifier) {
        return response(ownerRequest().post(WORKSPACE_APPLICATIONS + "/{identifier}/restore", workspaceIdentifier, identifier).then(),
                ApplicationClientResponse::new);
    }

    public ApplicationClientResponse inactivate(String workspaceIdentifier, String identifier) {
        return response(authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "workspace-it").accept(ContentType.JSON)
                .post(WORKSPACE_APPLICATIONS + "/{identifier}/inactivate", workspaceIdentifier, identifier).then(),
                ApplicationClientResponse::new);
    }

    public ApplicationClientResponse delete(String workspaceIdentifier, String identifier) {
        return response(authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "workspace-it").accept(ContentType.JSON)
                .delete(WORKSPACE_APPLICATIONS + "/{identifier}", workspaceIdentifier, identifier).then(),
                ApplicationClientResponse::new);
    }

    private io.restassured.specification.RequestSpecification ownerRequest() {
        return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "workspace-it")
                .contentType(ContentType.JSON).accept(ContentType.JSON);
    }

    public final class FindAllAction {
        private final String workspaceIdentifier;
        private final io.restassured.specification.RequestSpecification request = ownerRequest();
        private FindAllAction(String workspaceIdentifier) { this.workspaceIdentifier = workspaceIdentifier; }
        public FindAllAction lifecycle(String value) { request.queryParam("lifecycle", value); return this; }
        public FindAllAction tagName(String value) { request.queryParam("tagName", value); return this; }
        public FindAllAction page(int value) { request.queryParam("page", value); return this; }
        public FindAllAction size(int value) { request.queryParam("size", value); return this; }
        public FindAllAction sort(String value) { request.queryParam("sort", value); return this; }
        public ApplicationClientResponse execute() {
            return response(request.get(WORKSPACE_APPLICATIONS, workspaceIdentifier).then(), ApplicationClientResponse::new);
        }
    }
}
