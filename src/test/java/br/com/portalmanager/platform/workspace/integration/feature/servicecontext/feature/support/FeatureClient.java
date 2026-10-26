package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.feature.request.CreateFeatureRequest;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.feature.request.UpdateFeatureRequest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class FeatureClient extends BaseClient {
    private static final String PATH = "/api/v1/service-contexts/features";
    public FeatureClient(PlatformRequestSpecificationFactory requests) { super(requests, JsonMapper.builder().build()); }
    public FeatureClientResponse create(CreateFeatureRequest body) { return response(owner().body(json(body)).post(PATH).then(), FeatureClientResponse::new); }
    public FeatureClientResponse findByIdentifier(String identifier) { return response(owner().get(PATH + "/{identifier}", identifier).then(), FeatureClientResponse::new); }
    public FindAllAction findAll() { return new FindAllAction(); }
    public FeatureClientResponse update(String identifier, UpdateFeatureRequest body) { return response(owner().body(json(body)).put(PATH + "/{identifier}", identifier).then(), FeatureClientResponse::new); }
    public FeatureClientResponse activate(String identifier) { return response(owner().patch(PATH + "/{identifier}/activate", identifier).then(), FeatureClientResponse::new); }
    public FeatureClientResponse inactivate(String identifier) { return response(owner().patch(PATH + "/{identifier}/inactivate", identifier).then(), FeatureClientResponse::new); }
    private RequestSpecification owner() { return authorizedRequest(AuthorizationRequestData.builder().token("service-context-it").build()).header("correlation-id", "service-context-it").contentType(ContentType.JSON).accept(ContentType.JSON); }
    public final class FindAllAction {
        private final RequestSpecification request = owner();
        public FindAllAction lifecycle(String value) { request.queryParam("lifecycle", value); return this; }
        public FindAllAction featureGroup(String value) { request.queryParam("featureGroupIdentifier", value); return this; }
        public FindAllAction page(int value) { request.queryParam("page", value); return this; }
        public FindAllAction size(int value) { request.queryParam("size", value); return this; }
        public FindAllAction sort(String value) { request.queryParam("sort", value); return this; }
        public FeatureClientResponse execute() { return response(request.get(PATH).then(), FeatureClientResponse::new); }
    }
}
