package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Microservice Activate")
class MicroserviceActivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private MicroserviceScenario scenario;
    @Autowired private MicroserviceClient microservices;
    private final MicroserviceFactory factory = new MicroserviceFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("ACTIVATE - reativa microsserviço inativo")
    void reativaMicrosservico() {
        var created = microservices.create(factory.valid()).expectCreated().extractBody();
        microservices.inactivate(created.identifier()).expectOk();
        microservices.activate(created.identifier()).expectOk().expect("lifecycle", "ACTIVE");
    }

    @Test @DisplayName("VALIDATION - rejeita ativação de microsserviço ativo ou inexistente")
    void rejeitaAtivacaoInvalida() {
        var created = microservices.create(factory.valid()).expectCreated().extractBody();
        microservices.activate(created.identifier()).expectBadRequest();
        microservices.activate("00000000-0000-0000-0000-000000000000").expectNotFound();
    }
}
