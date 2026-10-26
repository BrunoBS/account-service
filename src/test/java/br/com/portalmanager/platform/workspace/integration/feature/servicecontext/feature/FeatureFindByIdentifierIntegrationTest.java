package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Find")
class FeatureFindByIdentifierIntegrationTest extends BaseIntegrationTest {
    @Autowired private FeatureScenario scenario;
    @Autowired private FeatureClient features;
    private final FeatureFactory factory = new FeatureFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("QUERY - encontra feature ativa por identificador")
    void encontraFeature() {
        var created = features.create(factory.valid(scenario.microserviceIdentifier())).expectCreated().extractBody();
        features.findByIdentifier(created.identifier()).expectOk().expect("code", created.code()).expect("settings", java.util.Map.of());
    }

    @Test @DisplayName("QUERY - não encontra feature inexistente ou inativa")
    void naoEncontraFeatureIndisponivel() {
        features.findByIdentifier("00000000-0000-0000-0000-000000000000").expectNotFound();
        var created = features.create(factory.valid(scenario.microserviceIdentifier())).expectCreated().extractBody();
        features.inactivate(created.identifier()).expectOk();
        features.findByIdentifier(created.identifier()).expectNotFound();
    }
}
