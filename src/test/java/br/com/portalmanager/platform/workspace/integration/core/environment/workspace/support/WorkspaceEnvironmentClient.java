package br.com.portalmanager.platform.workspace.integration.core.environment.workspace.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.CreateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.UpdateEnvironmentRequest;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentResponse;
import io.restassured.http.ContentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class WorkspaceEnvironmentClient extends BaseClient {
    private static final String PATH = "/api/v1/workspaces/{workspace}/environments";
    public WorkspaceEnvironmentClient(PlatformRequestSpecificationFactory requests) { super(requests, JsonMapper.builder().build()); }
    private io.restassured.specification.RequestSpecification owner() {
        return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build())
                .header("correlation-id", "environment-it").contentType(ContentType.JSON).accept(ContentType.JSON);
    }
    public EnvironmentResponse create(String workspace, CreateEnvironmentRequest body) { return response(owner().body(json(body)).post(PATH, workspace).then(), EnvironmentResponse::new); }
    public EnvironmentResponse find(String workspace, String id) { return response(owner().get(PATH+"/{id}",workspace,id).then(),EnvironmentResponse::new); }
    public EnvironmentResponse update(String workspace,String id,UpdateEnvironmentRequest body) { return response(owner().body(json(body)).put(PATH+"/{id}",workspace,id).then(),EnvironmentResponse::new); }
    public EnvironmentResponse inactivate(String workspace,String id) { return response(owner().post(PATH+"/{id}/inactivate",workspace,id).then(),EnvironmentResponse::new); }
    public EnvironmentResponse restore(String workspace,String id) { return response(owner().post(PATH+"/{id}/restore",workspace,id).then(),EnvironmentResponse::new); }
    public EnvironmentResponse delete(String workspace,String id) { return response(owner().delete(PATH+"/{id}",workspace,id).then(),EnvironmentResponse::new); }
    public EnvironmentResponse list(String workspace,String lifecycle) { return response(owner().queryParam("lifecycle",lifecycle).get(PATH,workspace).then(),EnvironmentResponse::new); }
    public EnvironmentResponse roots(String workspace) { return response(owner().get(PATH+"/roots",workspace).then(),EnvironmentResponse::new); }
    public EnvironmentResponse children(String workspace,String id) { return response(owner().get(PATH+"/{id}/children",workspace,id).then(),EnvironmentResponse::new); }
    public EnvironmentResponse tree(String workspace) { return response(owner().get(PATH+"/tree",workspace).then(),EnvironmentResponse::new); }
}
