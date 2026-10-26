package br.com.portalmanager.platform.workspace.integration.feature.segmentation.category.support;

import br.com.portalmanager.platform.library.testing.http.AuthorizationRequestData;
import br.com.portalmanager.platform.library.testing.http.BaseClient;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.category.request.CreateCategoryRequest;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.category.request.UpdateCategoryRequest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public final class CategoryClient extends BaseClient {
    public CategoryClient(PlatformRequestSpecificationFactory requests) { super(requests, JsonMapper.builder().build()); }
    private String path(String segment) { return "/api/v1/segmentation/segments/" + segment + "/categories"; }
    public CategoryClientResponse create(String segment, CreateCategoryRequest body) { return response(owner().body(json(body)).post(path(segment)).then(), CategoryClientResponse::new); }
    public CategoryClientResponse findByIdentifier(String segment, String id) { return response(owner().get(path(segment) + "/{id}", id).then(), CategoryClientResponse::new); }
    public CategoryClientResponse findAll(String segment, Map<String, Object> filters) { return response(owner().queryParams(filters).get(path(segment)).then(), CategoryClientResponse::new); }
    public CategoryClientResponse update(String segment, String id, UpdateCategoryRequest body) { return response(owner().body(json(body)).put(path(segment) + "/{id}", id).then(), CategoryClientResponse::new); }
    public CategoryClientResponse activate(String segment, String id) { return response(owner().patch(path(segment) + "/{id}/activate", id).then(), CategoryClientResponse::new); }
    public CategoryClientResponse inactivate(String segment, String id) { return response(owner().patch(path(segment) + "/{id}/inactivate", id).then(), CategoryClientResponse::new); }
    private io.restassured.specification.RequestSpecification owner() { return authorizedRequest(AuthorizationRequestData.builder().token("segmentation-it").build()).header("correlation-id", "segmentation-it").contentType(ContentType.JSON).accept(ContentType.JSON); }
}
