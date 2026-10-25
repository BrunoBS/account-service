package br.com.portalmanager.platform.workspace.integration.core.environment.compatibility.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.environment.request.CreateEnvironmentTypeCompatibilityRequest;
import io.restassured.http.ContentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class EnvironmentTypeCompatibilityClient extends BaseClient {
    private static final String PATH="/api/v1/environment-types/compatibilities";
    public EnvironmentTypeCompatibilityClient(PlatformRequestSpecificationFactory requests){super(requests,JsonMapper.builder().build());}
    private io.restassured.specification.RequestSpecification owner(){return authorizedRequest(AuthorizationRequestData.builder().token("owner-it").build()).header("correlation-id","environment-it").contentType(ContentType.JSON).accept(ContentType.JSON);}
    public EnvironmentTypeCompatibilityResponse create(CreateEnvironmentTypeCompatibilityRequest body){return response(owner().body(json(body)).post(PATH).then(),EnvironmentTypeCompatibilityResponse::new);}
    public EnvironmentTypeCompatibilityResponse find(String id){return response(owner().get(PATH+"/{id}",id).then(),EnvironmentTypeCompatibilityResponse::new);}
    public EnvironmentTypeCompatibilityResponse inactivate(String id){return response(owner().post(PATH+"/{id}/inactivate",id).then(),EnvironmentTypeCompatibilityResponse::new);}
    public EnvironmentTypeCompatibilityResponse delete(String id){return response(owner().delete(PATH+"/{id}",id).then(),EnvironmentTypeCompatibilityResponse::new);}
    public EnvironmentTypeCompatibilityResponse list(){return response(owner().get(PATH).then(),EnvironmentTypeCompatibilityResponse::new);}
    public EnvironmentTypeCompatibilityResponse list(String lifecycle){return response(owner().queryParam("lifecycle",lifecycle).get(PATH).then(),EnvironmentTypeCompatibilityResponse::new);}
}
