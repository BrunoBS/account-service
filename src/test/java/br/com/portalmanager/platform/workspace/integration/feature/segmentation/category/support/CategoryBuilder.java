package br.com.portalmanager.platform.workspace.integration.feature.segmentation.category.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.category.request.CreateCategoryRequest;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.category.request.UpdateCategoryRequest;
import java.util.UUID;

public final class CategoryBuilder implements TestDataBuilder<CreateCategoryRequest> {
    private Long version;
    private String code = "category-" + UUID.randomUUID().toString().substring(0, 8);
    private String name = "Category integration test";
    public static CategoryBuilder builder() { return new CategoryBuilder(); }
    public CategoryBuilder withVersion(Long value) { version = value; return this; }
    public CategoryBuilder withCode(String value) { code = value; return this; }
    public CategoryBuilder withName(String value) { name = value; return this; }
    @Override public CreateCategoryRequest build() { return new CreateCategoryRequest(version, code, name); }
    public UpdateCategoryRequest buildUpdate() { return new UpdateCategoryRequest(version, code, name); }
}
