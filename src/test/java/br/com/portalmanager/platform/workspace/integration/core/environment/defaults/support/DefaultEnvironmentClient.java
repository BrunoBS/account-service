package br.com.portalmanager.platform.workspace.integration.core.environment.defaults.support;

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
public final class DefaultEnvironmentClient extends BaseClient {
    private static final String PATH="/api/v1/environment-defaults";
    public DefaultEnvironmentClient(PlatformRequestSpecificationFactory requests) { super(requests,JsonMapper.builder().build()); }
    private io.restassured.specification.RequestSpecification owner(){return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build()).header("correlation-id","environment-it").contentType(ContentType.JSON).accept(ContentType.JSON);}
    public EnvironmentResponse create(CreateEnvironmentRequest body){return response(owner().body(json(body)).post(PATH).then(),EnvironmentResponse::new);}
    public EnvironmentResponse find(String id){return response(owner().get(PATH+"/{id}",id).then(),EnvironmentResponse::new);}
    public EnvironmentResponse update(String id,UpdateEnvironmentRequest body){return response(owner().body(json(body)).put(PATH+"/{id}",id).then(),EnvironmentResponse::new);}
    public EnvironmentResponse inactivate(String id){return response(owner().post(PATH+"/{id}/inactivate",id).then(),EnvironmentResponse::new);}
    public EnvironmentResponse restore(String id){return response(owner().post(PATH+"/{id}/restore",id).then(),EnvironmentResponse::new);}
    public EnvironmentResponse delete(String id){return response(owner().delete(PATH+"/{id}",id).then(),EnvironmentResponse::new);}
    public EnvironmentResponse list(String lifecycle){return response(owner().queryParam("lifecycle",lifecycle).get(PATH).then(),EnvironmentResponse::new);}
}
