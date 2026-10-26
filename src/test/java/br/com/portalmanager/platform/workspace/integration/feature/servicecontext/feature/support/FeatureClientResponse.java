package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.feature.response.FeatureResponse;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matchers;

public final class FeatureClientResponse extends BaseResponse<FeatureClientResponse> {
    public FeatureClientResponse(ValidatableResponse response) { super(response); }
    public FeatureResponse extractBody() { return extract(FeatureResponse.class); }
    public FeatureClientResponse expectContainsItem(String path, Object value) { response.body(path, Matchers.hasItem(value)); return this; }
    public FeatureClientResponse expectNotContainsItem(String path, Object value) { response.body(path, Matchers.not(Matchers.hasItem(value))); return this; }
}
