package br.com.portalmanager.platform.workspace.integration.feature.segmentation.category.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.category.request.CreateCategoryRequest;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.category.request.UpdateCategoryRequest;

public final class CategoryFactory extends AbstractTestDataFactory<CreateCategoryRequest, CategoryBuilder> {
    @Override protected CategoryBuilder builder() { return CategoryBuilder.builder(); }
    public CreateCategoryRequest valid() { return builder().build(); }
    public CreateCategoryRequest valid(String code) { return builder().withCode(code).build(); }
    public CreateCategoryRequest withoutCode() { return builder().withCode(null).build(); }
    public UpdateCategoryRequest update(Long version, String code, String name) {
        return builder().withVersion(version).withCode(code).withName(name).buildUpdate();
    }
}
