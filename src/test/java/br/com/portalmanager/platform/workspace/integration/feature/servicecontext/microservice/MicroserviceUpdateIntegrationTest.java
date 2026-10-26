package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice;

import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Microservice Update")
class MicroserviceUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private MicroserviceScenario scenario;
    @Autowired private MicroserviceClient microservices;
    private final MicroserviceFactory factory = new MicroserviceFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("UPDATE - atualiza os dados e incrementa a versão")
    void atualizaMicrosservico() {
        var created = microservices.create(factory.valid()).expectCreated().extractBody();
        var updated = microservices.update(created.identifier(), factory.update(created.version(), created.code(), "Updated service"))
                .expectOk().expect("name", "Updated service").extractBody();
        org.junit.jupiter.api.Assertions.assertEquals(created.version() + 1, updated.version());
        microservices.findByIdentifier(created.identifier()).expectOk().expect("name", "Updated service");
    }

    @Test @DisplayName("VALIDATION - rejeita versão ausente, obsoleta e código imutável")
    void rejeitaVersaoEAlteracaoDeCodigo() {
        var created = microservices.create(factory.valid()).expectCreated().extractBody();
        microservices.update(created.identifier(), factory.updateWithoutVersion(created.code(), "Missing version")).expectBadRequest();
        var update = factory.update(created.version(), created.code(), "Current name");
        microservices.update(created.identifier(), update).expectOk();
        microservices.update(created.identifier(), update).expectConflict();
        var current = microservices.findByIdentifier(created.identifier()).expectOk().extractBody();
        microservices.update(created.identifier(), factory.updateWithChangedCode(current.version())).expectBadRequest().expectContainsItem("details.field", "code");
    }
}
