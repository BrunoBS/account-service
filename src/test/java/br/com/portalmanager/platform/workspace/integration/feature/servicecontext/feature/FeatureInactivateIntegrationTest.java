package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Inactivate")
class FeatureInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private FeatureScenario scenario;
    @Autowired private FeatureClient features;
    private final FeatureFactory factory = new FeatureFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("INACTIVATE - inativa feature e a encontra no filtro INACTIVE")
    void inativaFeature() {
        var created = features.create(factory.valid(scenario.microserviceIdentifier())).expectCreated().extractBody();
        features.inactivate(created.identifier()).expectOk().expect("lifecycle", "INACTIVE");
        features.findByIdentifier(created.identifier()).expectNotFound();
        features.findAll().lifecycle("INACTIVE").execute().expectOk().expectContainsItem("content.identifier", created.identifier());
    }

    @Test @DisplayName("VALIDATION - rejeita inativar feature já inativa")
    void rejeitaInativacaoRepetida() {
        var created = features.create(factory.valid(scenario.microserviceIdentifier())).expectCreated().extractBody();
        features.inactivate(created.identifier()).expectOk();
        features.inactivate(created.identifier()).expectNotFound();
    }
}
