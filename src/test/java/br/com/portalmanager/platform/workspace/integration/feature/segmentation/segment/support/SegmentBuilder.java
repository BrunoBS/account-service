package br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment.support;

import br.com.portalmanager.platform.library.testing.fixture.builder.TestDataBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.segment.request.CreateSegmentRequest;
import br.com.portalmanager.platform.workspace.entrypoint.segmentation.segment.request.UpdateSegmentRequest;
import java.util.UUID;

public final class SegmentBuilder implements TestDataBuilder<CreateSegmentRequest> {
    private Long version;
    private String code = "segment-" + UUID.randomUUID().toString().substring(0, 8);
    private String name = "Segment integration test";
    public static SegmentBuilder builder() { return new SegmentBuilder(); }
    public SegmentBuilder withVersion(Long value) { version = value; return this; }
    public SegmentBuilder withCode(String value) { code = value; return this; }
    public SegmentBuilder withName(String value) { name = value; return this; }
    @Override public CreateSegmentRequest build() { return new CreateSegmentRequest(version, code, name); }
    public UpdateSegmentRequest buildUpdate() { return new UpdateSegmentRequest(version, code, name); }
}
