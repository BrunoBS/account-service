package br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.request.CreateWorkspaceEnvironmentConfigurationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.request.UpdateWorkspaceEnvironmentConfigurationRequest;
import io.restassured.http.ContentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class WorkspaceEnvironmentConfigurationClient extends BaseClient {
    private static final String PATH = "/api/v1/workspaces/{workspace}/environment-configurations";

    public WorkspaceEnvironmentConfigurationClient(PlatformRequestSpecificationFactory requests) {
        super(requests, JsonMapper.builder().build());
    }

    private io.restassured.specification.RequestSpecification owner() {
        return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "workspace-environment-configuration-it")
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON);
    }

    public WorkspaceEnvironmentConfigurationResponse create(
            String workspace, CreateWorkspaceEnvironmentConfigurationRequest body) {
        return response(owner().body(json(body)).post(PATH, workspace).then(),
                WorkspaceEnvironmentConfigurationResponse::new);
    }

    public WorkspaceEnvironmentConfigurationResponse update(
            String workspace, String identifier, UpdateWorkspaceEnvironmentConfigurationRequest body) {
        return response(owner().body(json(body)).put(PATH + "/{id}", workspace, identifier).then(),
                WorkspaceEnvironmentConfigurationResponse::new);
    }

    public WorkspaceEnvironmentConfigurationResponse find(String workspace, String identifier) {
        return response(owner().get(PATH + "/{id}", workspace, identifier).then(),
                WorkspaceEnvironmentConfigurationResponse::new);
    }

    public FindAllAction findAll(String workspace) {
        return new FindAllAction(workspace);
    }

    public WorkspaceEnvironmentConfigurationResponse inactivate(String workspace, String identifier) {
        return response(owner().post(PATH + "/{id}/inactivate", workspace, identifier).then(),
                WorkspaceEnvironmentConfigurationResponse::new);
    }

    public WorkspaceEnvironmentConfigurationResponse delete(String workspace, String identifier) {
        return response(owner().delete(PATH + "/{id}", workspace, identifier).then(),
                WorkspaceEnvironmentConfigurationResponse::new);
    }

    public final class FindAllAction {
        private final String workspace;
        private final io.restassured.specification.RequestSpecification request = owner();

        private FindAllAction(String workspace) {
            this.workspace = workspace;
        }

        public FindAllAction lifecycle(String value) {
            request.queryParam("lifecycle", value);
            return this;
        }

        public FindAllAction page(int value) {
            request.queryParam("page", value);
            return this;
        }

        public FindAllAction size(int value) {
            request.queryParam("size", value);
            return this;
        }

        public FindAllAction sort(String value) {
            request.queryParam("sort", value);
            return this;
        }

        public WorkspaceEnvironmentConfigurationResponse execute() {
            return response(request.get(PATH, workspace).then(), WorkspaceEnvironmentConfigurationResponse::new);
        }
    }
}
