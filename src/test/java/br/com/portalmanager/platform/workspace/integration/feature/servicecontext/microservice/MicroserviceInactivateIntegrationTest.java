package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Microservice Inactivate")
class MicroserviceInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private MicroserviceScenario scenario;
    @Autowired private MicroserviceClient microservices;
    private final MicroserviceFactory factory = new MicroserviceFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("INACTIVATE - inativa microsserviço e o remove da consulta ativa")
    void inativaMicrosservico() {
        var created = microservices.create(factory.valid()).expectCreated().extractBody();
        microservices.inactivate(created.identifier()).expectOk().expect("lifecycle", "INACTIVE");
        microservices.findByIdentifier(created.identifier()).expectNotFound();
        microservices.findAll().lifecycle("INACTIVE").execute().expectOk().expectContainsItem("content.identifier", created.identifier());
    }

    @Test @DisplayName("VALIDATION - não inativa duas vezes")
    void rejeitaInativacaoRepetida() {
        var created = microservices.create(factory.valid()).expectCreated().extractBody();
        microservices.inactivate(created.identifier()).expectOk();
        microservices.inactivate(created.identifier()).expectNotFound();
    }
}
