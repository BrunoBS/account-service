package br.com.portalmanager.platform.workspace.entrypoint.web.segmentation.segment.request;

import br.com.portalmanager.platform.library.schemavalidation.annotation.ValidateResourceSchemaRequest;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.model.segment.UpdateSegmentInput;
import br.com.portalmanager.platform.workspace.feature.segmentation.usecase.operations.SegmentNormalizer;

@ValidateResourceSchemaRequest(
        type = "SEGMENTATION",
        code = "segment",
        normalizer = SegmentNormalizer.class)
public record UpdateSegmentRequest(Long version, String code, String name) {
    public UpdateSegmentInput toInput() {
        return new UpdateSegmentInput(version, code, name);
    }
}
