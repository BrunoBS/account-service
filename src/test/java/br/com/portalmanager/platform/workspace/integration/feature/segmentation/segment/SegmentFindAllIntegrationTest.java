package br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment;

import br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment.support.*;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SegmentFindAllIntegrationTest extends BaseIntegrationTest {
    @Autowired private SegmentClient segments;
    @Autowired private SegmentScenario scenario;
    private final SegmentFactory factory = new SegmentFactory();
    @BeforeEach void prepare() { scenario.setup(); }
    @Test @DisplayName("QUERY - lista segmentos filtrados por código") void listaPorCodigo() {
        segments.create(factory.valid("mobile-listing")).expectCreated();
        segments.findAll(Map.of("code", "mobile-listing", "page", 0, "size", 10)).expectOk();
    }
}
