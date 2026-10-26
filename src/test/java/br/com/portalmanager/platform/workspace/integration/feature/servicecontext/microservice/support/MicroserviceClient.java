package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.microservice.request.CreateMicroserviceRequest;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.microservice.request.UpdateMicroserviceRequest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class MicroserviceClient extends BaseClient {
    private static final String PATH = "/api/v1/service-contexts/microservices";
    public MicroserviceClient(PlatformRequestSpecificationFactory requests) { super(requests, JsonMapper.builder().build()); }
    public MicroserviceClientResponse create(CreateMicroserviceRequest body) { return response(owner().body(json(body)).post(PATH).then(), MicroserviceClientResponse::new); }
    public MicroserviceClientResponse findByIdentifier(String identifier) { return response(owner().get(PATH + "/{identifier}", identifier).then(), MicroserviceClientResponse::new); }
    public FindAllAction findAll() { return new FindAllAction(); }
    public MicroserviceClientResponse update(String identifier, UpdateMicroserviceRequest body) { return response(owner().body(json(body)).put(PATH + "/{identifier}", identifier).then(), MicroserviceClientResponse::new); }
    public MicroserviceClientResponse activate(String identifier) { return response(owner().patch(PATH + "/{identifier}/activate", identifier).then(), MicroserviceClientResponse::new); }
    public MicroserviceClientResponse inactivate(String identifier) { return response(owner().patch(PATH + "/{identifier}/inactivate", identifier).then(), MicroserviceClientResponse::new); }
    public MicroserviceClientResponse purge(String identifier) { return response(owner().delete(PATH + "/{identifier}", identifier).then(), MicroserviceClientResponse::new); }
    private RequestSpecification owner() { return authorizedRequest(AuthorizationRequestData.builder().token("service-context-it").build()).header("correlation-id", "service-context-it").contentType(ContentType.JSON).accept(ContentType.JSON); }
    public final class FindAllAction {
        private final RequestSpecification request = owner();
        public FindAllAction lifecycle(String value) { request.queryParam("lifecycle", value); return this; }
        public FindAllAction page(int value) { request.queryParam("page", value); return this; }
        public FindAllAction size(int value) { request.queryParam("size", value); return this; }
        public FindAllAction sort(String value) { request.queryParam("sort", value); return this; }
        public MicroserviceClientResponse execute() { return response(request.get(PATH).then(), MicroserviceClientResponse::new); }
    }
}
