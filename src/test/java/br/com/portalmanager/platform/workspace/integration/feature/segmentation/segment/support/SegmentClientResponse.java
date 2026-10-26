package br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.segment.response.SegmentResponse;
import io.restassured.response.ValidatableResponse;

public final class SegmentClientResponse extends BaseResponse<SegmentClientResponse> {
    public SegmentClientResponse(ValidatableResponse response) { super(response); }
    public String identifier() { return response.extract().path("identifier"); }
    public SegmentResponse extractBody() { return extract(SegmentResponse.class); }
}
