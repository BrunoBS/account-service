package br.com.portalmanager.platform.workspace.integration.feature.segmentation.category.support;

import br.com.portalmanager.platform.library.testing.fixture.scenario.TestScenario;
import br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment.support.SegmentScenario;
import java.util.Objects;
import org.springframework.stereotype.Component;

/** Creates a segment through HTTP for category operations. */
@Component
public final class CategoryScenario implements TestScenario<String> {
    private final SegmentScenario common;
    public CategoryScenario(SegmentScenario common) {
        this.common = Objects.requireNonNull(common);
    }
    @Override public String setup() { return common.setup(); }
}
