package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.featuregroup.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.featuregroup.response.FeatureGroupResponse;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matchers;

public final class FeatureGroupClientResponse extends BaseResponse<FeatureGroupClientResponse> {
    public FeatureGroupClientResponse(ValidatableResponse response) { super(response); }
    public FeatureGroupResponse extractBody() { return extract(FeatureGroupResponse.class); }
    public FeatureGroupClientResponse expectContainsItem(String path, Object value) { response.body(path, Matchers.hasItem(value)); return this; }
    public FeatureGroupClientResponse expectNotContainsItem(String path, Object value) { response.body(path, Matchers.not(Matchers.hasItem(value))); return this; }
}
