package br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment;

import br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment.support.*;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SegmentFindByIdentifierIntegrationTest extends BaseIntegrationTest {
    @Autowired private SegmentClient segments;
    @Autowired private SegmentScenario scenario;
    private String identifier;
    @BeforeEach void prepare() { identifier = scenario.setup(); }
    @Test @DisplayName("QUERY - consulta segmento pelo identificador") void encontraSegmento() {
        segments.findByIdentifier(identifier).expectOk().expect("identifier", identifier);
    }
    @Test @DisplayName("QUERY - retorna não encontrado para segmento ausente") void segmentoAusente() {
        segments.findByIdentifier("missing-segment").expectNotFound();
    }
}
