package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Microservice Find")
class MicroserviceFindByIdentifierIntegrationTest extends BaseIntegrationTest {
    @Autowired private MicroserviceScenario scenario;
    @Autowired private MicroserviceClient microservices;
    private final MicroserviceFactory factory = new MicroserviceFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("QUERY - encontra microsserviço pelo identificador")
    void encontraMicrosservico() {
        var created = microservices.create(factory.valid()).expectCreated().extractBody();
        microservices.findByIdentifier(created.identifier()).expectOk().expect("identifier", created.identifier()).expect("settings", java.util.Map.of());
    }

    @Test @DisplayName("QUERY - não expõe microsserviço inexistente ou inativo")
    void ocultaMicrosservicoInexistenteOuInativo() {
        microservices.findByIdentifier("00000000-0000-0000-0000-000000000000").expectNotFound();
        var created = microservices.create(factory.valid()).expectCreated().extractBody();
        microservices.inactivate(created.identifier()).expectOk();
        microservices.findByIdentifier(created.identifier()).expectNotFound();
    }
}
