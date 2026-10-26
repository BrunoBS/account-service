package br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment;

import br.com.portalmanager.platform.workspace.integration.feature.segmentation.segment.support.*;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SegmentUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private SegmentClient segments;
    @Autowired private SegmentScenario scenario;
    private final SegmentFactory factory = new SegmentFactory();
    private String identifier;
    @BeforeEach void prepare() { identifier = scenario.setup(); }
    @Test @DisplayName("UPDATE - atualiza nome mantendo o código") void atualizaNome() {
        var current = segments.findByIdentifier(identifier).expectOk().extractBody();
        segments.update(identifier, factory.update(current.version(), current.code(), "Updated segment name")).expectOk();
        segments.findByIdentifier(identifier).expectOk().expect("name", "Updated segment name");
    }
    @Test @DisplayName("UPDATE - rejeita versão obsoleta") void rejeitaVersaoObsoleta() {
        var current = segments.findByIdentifier(identifier).expectOk().extractBody();
        segments.update(identifier, factory.update(current.version() + 10, current.code(), "Stale update")).expectConflict();
    }
}
