package br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment;

import br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment.support.*;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SegmentInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private SegmentClient segments;
    @Autowired private SegmentScenario scenario;
    private String identifier;
    @BeforeEach void prepare() { identifier = scenario.setup(); }
    @Test @DisplayName("LIFECYCLE - inativa segmento ativo") void inativaSegmentoAtivo() {
        segments.inactivate(identifier).expectOk();
        segments.findByIdentifier(identifier).expectOk().expect("lifecycle", "INACTIVE");
    }
}
