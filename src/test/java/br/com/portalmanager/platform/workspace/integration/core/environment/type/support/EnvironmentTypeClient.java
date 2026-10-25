package br.com.portalmanager.platform.workspace.integration.core.environment.type.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.CreateEnvironmentTypeRequest;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.UpdateEnvironmentTypeRequest;
import io.restassured.http.ContentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class EnvironmentTypeClient extends BaseClient {
    private static final String PATH="/api/v1/environment-types";
    public EnvironmentTypeClient(PlatformRequestSpecificationFactory requests){super(requests,JsonMapper.builder().build());}
    private io.restassured.specification.RequestSpecification owner(){return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build()).header("correlation-id","environment-it").contentType(ContentType.JSON).accept(ContentType.JSON);}
    public EnvironmentTypeResponse create(CreateEnvironmentTypeRequest body){return response(owner().body(json(body)).post(PATH).then(),EnvironmentTypeResponse::new);}
    public EnvironmentTypeResponse find(String code){return response(owner().get(PATH+"/{code}",code).then(),EnvironmentTypeResponse::new);}
    public EnvironmentTypeResponse update(String code,UpdateEnvironmentTypeRequest body){return response(owner().body(json(body)).put(PATH+"/{code}",code).then(),EnvironmentTypeResponse::new);}
    public EnvironmentTypeResponse inactivate(String code){return response(owner().post(PATH+"/{code}/inactivate",code).then(),EnvironmentTypeResponse::new);}
    public EnvironmentTypeResponse restore(String code){return response(owner().post(PATH+"/{code}/restore",code).then(),EnvironmentTypeResponse::new);}
    public EnvironmentTypeResponse delete(String code){return response(owner().delete(PATH+"/{code}",code).then(),EnvironmentTypeResponse::new);}
    public EnvironmentTypeResponse list(String lifecycle){return response(owner().queryParam("lifecycle",lifecycle).get(PATH).then(),EnvironmentTypeResponse::new);}
}
