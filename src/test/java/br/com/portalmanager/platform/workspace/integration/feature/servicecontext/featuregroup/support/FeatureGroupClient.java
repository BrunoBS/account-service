package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.featuregroup.request.CreateFeatureGroupRequest;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.featuregroup.request.UpdateFeatureGroupRequest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class FeatureGroupClient extends BaseClient {
    private static final String PATH = "/api/v1/service-contexts/feature-groups";
    public FeatureGroupClient(PlatformRequestSpecificationFactory requests) { super(requests, JsonMapper.builder().build()); }
    public FeatureGroupClientResponse create(CreateFeatureGroupRequest body) { return response(owner().body(json(body)).post(PATH).then(), FeatureGroupClientResponse::new); }
    public FeatureGroupClientResponse findByIdentifier(String identifier) { return response(owner().get(PATH + "/{identifier}", identifier).then(), FeatureGroupClientResponse::new); }
    public FindAllAction findAll() { return new FindAllAction(); }
    public FeatureGroupClientResponse update(String identifier, UpdateFeatureGroupRequest body) { return response(owner().body(json(body)).put(PATH + "/{identifier}", identifier).then(), FeatureGroupClientResponse::new); }
    public FeatureGroupClientResponse activate(String identifier) { return response(owner().patch(PATH + "/{identifier}/activate", identifier).then(), FeatureGroupClientResponse::new); }
    public FeatureGroupClientResponse inactivate(String identifier) { return response(owner().patch(PATH + "/{identifier}/inactivate", identifier).then(), FeatureGroupClientResponse::new); }
    public FeatureGroupClientResponse purge(String identifier) { return response(owner().delete(PATH + "/{identifier}", identifier).then(), FeatureGroupClientResponse::new); }
    private RequestSpecification owner() { return authorizedRequest(AuthorizationRequestData.builder().token("service-context-it").build()).header("correlation-id", "service-context-it").contentType(ContentType.JSON).accept(ContentType.JSON); }
    public final class FindAllAction {
        private final RequestSpecification request = owner();
        public FindAllAction lifecycle(String value) { request.queryParam("lifecycle", value); return this; }
        public FindAllAction feature(String value) { request.queryParam("featureIdentifier", value); return this; }
        public FindAllAction page(int value) { request.queryParam("page", value); return this; }
        public FindAllAction size(int value) { request.queryParam("size", value); return this; }
        public FindAllAction sort(String value) { request.queryParam("sort", value); return this; }
        public FeatureGroupClientResponse execute() { return response(request.get(PATH).then(), FeatureGroupClientResponse::new); }
    }
}
