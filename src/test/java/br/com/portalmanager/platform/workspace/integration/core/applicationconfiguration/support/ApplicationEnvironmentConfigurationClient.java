package br.com.portalmanager.platform.workspace.integration.core.applicationconfiguration.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.request.CreateApplicationEnvironmentConfigurationRequest;
import br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.request.UpdateApplicationEnvironmentConfigurationRequest;
import io.restassured.http.ContentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class ApplicationEnvironmentConfigurationClient extends BaseClient {
    private static final String PATH = "/api/v1/workspaces/{workspace}/applications/{application}/environment-configurations";
    public ApplicationEnvironmentConfigurationClient(PlatformRequestSpecificationFactory requests) { super(requests, JsonMapper.builder().build()); }
    private io.restassured.specification.RequestSpecification owner() {
        return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "application-environment-configuration-it")
                .contentType(ContentType.JSON).accept(ContentType.JSON);
    }
    public ApplicationEnvironmentConfigurationResponse create(String workspace, String application, CreateApplicationEnvironmentConfigurationRequest body) {
        return response(owner().body(json(body)).post(PATH, workspace, application).then(), ApplicationEnvironmentConfigurationResponse::new);
    }
    public ApplicationEnvironmentConfigurationResponse update(String workspace, String application, String id, UpdateApplicationEnvironmentConfigurationRequest body) {
        return response(owner().body(json(body)).put(PATH + "/{id}", workspace, application, id).then(), ApplicationEnvironmentConfigurationResponse::new);
    }
    public ApplicationEnvironmentConfigurationResponse find(String workspace, String application, String id) {
        return response(owner().get(PATH + "/{id}", workspace, application, id).then(), ApplicationEnvironmentConfigurationResponse::new);
    }
    public FindAllAction findAll(String workspace, String application) { return new FindAllAction(workspace, application); }
    public ApplicationEnvironmentConfigurationResponse inactivate(String workspace, String application, String id) {
        return response(owner().post(PATH + "/{id}/inactivate", workspace, application, id).then(), ApplicationEnvironmentConfigurationResponse::new);
    }
    public ApplicationEnvironmentConfigurationResponse delete(String workspace, String application, String id) {
        return response(owner().delete(PATH + "/{id}", workspace, application, id).then(), ApplicationEnvironmentConfigurationResponse::new);
    }
    public final class FindAllAction {
        private final String workspace; private final String application;
        private final io.restassured.specification.RequestSpecification request = owner();
        private FindAllAction(String workspace, String application) { this.workspace=workspace; this.application=application; }
        public FindAllAction lifecycle(String value) { request.queryParam("lifecycle", value); return this; }
        public FindAllAction page(int value) { request.queryParam("page", value); return this; }
        public FindAllAction size(int value) { request.queryParam("size", value); return this; }
        public FindAllAction sort(String value) { request.queryParam("sort", value); return this; }
        public ApplicationEnvironmentConfigurationResponse execute() {
            return response(request.get(PATH, workspace, application).then(), ApplicationEnvironmentConfigurationResponse::new);
        }
    }
}
