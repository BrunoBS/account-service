package br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment.support;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.support.SchemaDefaultScenario;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public final class SegmentScenario implements TestScenario<String> {
    private final AuthorizationMock authorization;
    private final SchemaDefaultScenario schemas;
    private final SegmentClient segments;
    private final SegmentFactory factory = new SegmentFactory();
    public SegmentScenario(AuthorizationMock authorization, SchemaDefaultScenario schemas, SegmentClient segments) {
        this.authorization = Objects.requireNonNull(authorization);
        this.schemas = Objects.requireNonNull(schemas);
        this.segments = Objects.requireNonNull(segments);
    }
    @Override public String setup() {
        authorization.reset();
        authorization.allow(session -> session.userName("segmentation-owner").groups("PM5_OWNER"));
        schemas.setup();
        return segments.create(factory.valid()).expectCreated().extractBody().identifier();
    }
}
