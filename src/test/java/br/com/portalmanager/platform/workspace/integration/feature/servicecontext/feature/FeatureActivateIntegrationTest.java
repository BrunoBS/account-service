package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureScenario;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Activate")
class FeatureActivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private FeatureScenario scenario;
    @Autowired private FeatureClient features;
    @Autowired private MicroserviceClient microservices;
    private final FeatureFactory factory = new FeatureFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("ACTIVATE - reativa feature inativa")
    void reativaFeature() {
        var created = features.create(factory.valid(scenario.microserviceIdentifier())).expectCreated().extractBody();
        features.inactivate(created.identifier()).expectOk();
        features.activate(created.identifier()).expectOk().expect("lifecycle", "ACTIVE");
    }

    @Test @DisplayName("VALIDATION - rejeita ativação de feature ativa, inexistente ou com microsserviço inativo")
    void rejeitaAtivacaoInvalida() {
        var active = features.create(factory.valid(scenario.microserviceIdentifier())).expectCreated().extractBody();
        features.activate(active.identifier()).expectBadRequest();
        features.activate("00000000-0000-0000-0000-000000000000").expectNotFound();

        var inactiveParent = scenario.createMicroservice("inactive-service");
        var feature = features.create(factory.valid(inactiveParent)).expectCreated().extractBody();
        microservices.inactivate(inactiveParent).expectOk();
        features.inactivate(feature.identifier()).expectOk();
        features.activate(feature.identifier()).expectBadRequest();
    }
}
