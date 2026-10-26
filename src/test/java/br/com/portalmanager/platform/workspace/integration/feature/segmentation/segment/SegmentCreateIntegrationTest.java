package br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment;

import br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment.support.*;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SegmentCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private SegmentClient segments;
    @Autowired private SegmentScenario scenario;
    private final SegmentFactory factory = new SegmentFactory();
    @BeforeEach void prepare() { scenario.setup(); }
    @Test @DisplayName("CREATE - cria segmento") void criaSegmento() {
        segments.create(factory.valid("mobile-audience")).expectCreated()
                .expect("code", "mobile-audience").expect("lifecycle", "ACTIVE");
    }
    @Test @DisplayName("VALIDATION - rejeita segmento sem código") void rejeitaCodigoAusente() {
        segments.create(factory.withoutCode()).expectBadRequest();
    }
}
