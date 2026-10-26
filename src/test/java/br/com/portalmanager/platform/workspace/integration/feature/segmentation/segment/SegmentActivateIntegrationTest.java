package br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment;

import br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment.support.*;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SegmentActivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private SegmentClient segments;
    @Autowired private SegmentScenario scenario;
    private String identifier;
    @BeforeEach void prepare() { identifier = scenario.setup(); }
    @Test @DisplayName("LIFECYCLE - ativa segmento inativo") void ativaSegmentoInativo() {
        segments.inactivate(identifier).expectOk();
        segments.activate(identifier).expectOk();
        segments.findByIdentifier(identifier).expectOk().expect("lifecycle", "ACTIVE");
    }
}
