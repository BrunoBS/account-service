package br.com.portalmanager.platform.workspace.integration.feature.segmentation.category.support;

import br.com.portalmanager.platform.library.testing.http.response.BaseResponse;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.category.response.CategoryResponse;
import io.restassured.response.ValidatableResponse;

public final class CategoryClientResponse extends BaseResponse<CategoryClientResponse> {
    public CategoryClientResponse(ValidatableResponse response) { super(response); }
    public String identifier() { return response.extract().path("identifier"); }
    public CategoryResponse extractBody() { return extract(CategoryResponse.class); }
}
