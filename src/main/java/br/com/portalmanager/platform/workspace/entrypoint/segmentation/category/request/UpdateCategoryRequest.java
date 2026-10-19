package br.com.portalmanager.platform.workspace.entrypoint.segmentation.category.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.UpdateCategoryInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.SegmentNormalizer;

@ValidateResourceSchemaRequest(
        type = "SEGMENTATION",
        code = "category",
        normalizer = SegmentNormalizer.class)
public record UpdateCategoryRequest(Long version, String code, String name) {
    public UpdateCategoryInput toInput() {
        return new UpdateCategoryInput(version, code, name);
    }
}
