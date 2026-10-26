package br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment.support;

import br.com.portalmanager.platform.library.testing.fixture.factory.AbstractTestDataFactory;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.segment.request.CreateSegmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.segment.request.UpdateSegmentRequest;

public final class SegmentFactory extends AbstractTestDataFactory<CreateSegmentRequest, SegmentBuilder> {
    @Override protected SegmentBuilder builder() { return SegmentBuilder.builder(); }
    public CreateSegmentRequest valid() { return builder().build(); }
    public CreateSegmentRequest valid(String code) { return builder().withCode(code).build(); }
    public CreateSegmentRequest withoutCode() { return builder().withCode(null).build(); }
    public UpdateSegmentRequest update(Long version, String code, String name) {
        return builder().withVersion(version).withCode(code).withName(name).buildUpdate();
    }
}
