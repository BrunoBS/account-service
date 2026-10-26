package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroupassociation.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureClientResponse;
import io.restassured.http.ContentType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class FeatureGroupAssociationClient extends BaseClient {
    private static final String PATH = "/api/v1/service-contexts/feature-groups/{group}/features/{feature}";
    public FeatureGroupAssociationClient(PlatformRequestSpecificationFactory requests) { super(requests, JsonMapper.builder().build()); }
    public FeatureClientResponse associate(String group, String feature) {
        return response(owner().post(PATH, group, feature).then(), FeatureClientResponse::new);
    }
    public FeatureClientResponse remove(String group, String feature) {
        return response(owner().delete(PATH, group, feature).then(), FeatureClientResponse::new);
    }
    private io.restassured.specification.RequestSpecification owner() {
        return authorizedRequest(AuthorizationRequestData.builder().token("service-context-it").build())
                .header("correlation-id", "service-context-it").contentType(ContentType.JSON).accept(ContentType.JSON);
    }
}
