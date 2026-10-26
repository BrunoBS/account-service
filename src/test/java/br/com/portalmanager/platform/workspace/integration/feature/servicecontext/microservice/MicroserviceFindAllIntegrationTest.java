package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Microservice FindAll")
class MicroserviceFindAllIntegrationTest extends BaseIntegrationTest {
    @Autowired private MicroserviceScenario scenario;
    @Autowired private MicroserviceClient microservices;
    private final MicroserviceFactory factory = new MicroserviceFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("QUERY - filtra por lifecycle e pagina os microsserviços")
    void filtraPorLifecycleEPagina() {
        var first = microservices.create(factory.valid()).expectCreated().extractBody();
        var second = microservices.create(factory.valid()).expectCreated().extractBody();
        microservices.inactivate(second.identifier()).expectOk();
        microservices.findAll().execute().expectOk().expectContainsItem("content.identifier", first.identifier()).expectNotContainsItem("content.identifier", second.identifier());
        microservices.findAll().lifecycle("INACTIVE").execute().expectOk().expectContainsItem("content.identifier", second.identifier());
        microservices.findAll().page(0).size(1).sort("code,asc").execute().expectOk().expectSize("content", 1);
    }

    @Test @DisplayName("VALIDATION - rejeita lifecycle e ordenação inválidos")
    void rejeitaFiltrosInvalidos() {
        microservices.findAll().lifecycle("UNKNOWN").execute().expectBadRequest();
        microservices.findAll().sort("settings").execute().expectBadRequest();
    }
}
