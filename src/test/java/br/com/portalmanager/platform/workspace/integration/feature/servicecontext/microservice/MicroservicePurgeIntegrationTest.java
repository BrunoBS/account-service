package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Microservice Purge")
class MicroservicePurgeIntegrationTest extends BaseIntegrationTest {
    @Autowired private MicroserviceScenario scenario;
    @Autowired private MicroserviceClient microservices;
    @Autowired private FeatureClient features;
    private final MicroserviceFactory factory = new MicroserviceFactory();
    private final FeatureFactory featureFactory = new FeatureFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("DELETE - remove microsserviço inativo sem recursos vinculados")
    void removeMicrosservicoInativo() {
        var created = microservices.create(factory.valid()).expectCreated().extractBody();
        microservices.purge(created.identifier()).expectBadRequest();
        microservices.inactivate(created.identifier()).expectOk();
        microservices.purge(created.identifier()).expectNoContent();
        microservices.findByIdentifier(created.identifier()).expectNotFound();
    }

    @Test @DisplayName("VALIDATION - impede excluir microsserviço que possui feature")
    void impedeExcluirMicrosservicoEmUso() {
        var created = microservices.create(factory.valid()).expectCreated().extractBody();
        features.create(featureFactory.valid(created.identifier())).expectCreated();
        microservices.inactivate(created.identifier()).expectOk();
        microservices.purge(created.identifier()).expectBadRequest().expect("code", "SERVICE-CONTEXT-0129");
    }
}
