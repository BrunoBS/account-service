package br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.segment.request.CreateSegmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.segment.request.UpdateSegmentRequest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class SegmentClient extends BaseClient {
    private static final String PATH = "/api/v1/segmentation/segments";
    public SegmentClient(PlatformRequestSpecificationFactory requests) { super(requests, JsonMapper.builder().build()); }
    public SegmentClientResponse create(CreateSegmentRequest body) { return response(owner().body(json(body)).post(PATH).then(), SegmentClientResponse::new); }
    public SegmentClientResponse findByIdentifier(String id) { return response(owner().get(PATH + "/{id}", id).then(), SegmentClientResponse::new); }
    public SegmentClientResponse findAll(Map<String, Object> filters) { return response(owner().queryParams(filters).get(PATH).then(), SegmentClientResponse::new); }
    public SegmentClientResponse update(String id, UpdateSegmentRequest body) { return response(owner().body(json(body)).put(PATH + "/{id}", id).then(), SegmentClientResponse::new); }
    public SegmentClientResponse activate(String id) { return response(owner().patch(PATH + "/{id}/activate", id).then(), SegmentClientResponse::new); }
    public SegmentClientResponse inactivate(String id) { return response(owner().patch(PATH + "/{id}/inactivate", id).then(), SegmentClientResponse::new); }
    private io.restassured.specification.RequestSpecification owner() { return authorizedRequest(AuthorizationRequestData.builder().token("segmentation-it").build()).header("correlation-id", "segmentation-it").contentType(ContentType.JSON).accept(ContentType.JSON); }
}
