package br.com.portalmanager.platform.workspace.entrypoint.segmentation.category.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.category.CreateCategoryInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.SegmentNormalizer;

@ValidateResourceSchemaRequest(
        type = "SEGMENTATION",
        code = "category",
        normalizer = SegmentNormalizer.class)
public record CreateCategoryRequest(Long version, String code, String name) {
    public CreateCategoryInput toInput(String segmentIdentifier) {
        return new CreateCategoryInput(version, code, name, segmentIdentifier);
    }
}
