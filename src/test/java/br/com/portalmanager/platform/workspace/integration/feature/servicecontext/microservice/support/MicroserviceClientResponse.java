package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import br.com.portalmanager.platform.workspace.entrypoint.servicecontext.microservice.response.MicroserviceResponse;
import io.restassured.response.ValidatableResponse;
import org.hamcrest.Matchers;

public final class MicroserviceClientResponse extends BaseResponse<MicroserviceClientResponse> {
    public MicroserviceClientResponse(ValidatableResponse response) { super(response); }
    public MicroserviceResponse extractBody() { return extract(MicroserviceResponse.class); }
    public MicroserviceClientResponse expectContainsItem(String path, Object value) { response.body(path, Matchers.hasItem(value)); return this; }
    public MicroserviceClientResponse expectNotContainsItem(String path, Object value) { response.body(path, Matchers.not(Matchers.hasItem(value))); return this; }
}
