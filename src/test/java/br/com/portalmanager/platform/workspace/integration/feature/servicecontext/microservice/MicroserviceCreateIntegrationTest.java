package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.microservice.support.MicroserviceScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Microservice Create")
class MicroserviceCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private MicroserviceScenario scenario;
    @Autowired private MicroserviceClient microservices;
    private final MicroserviceFactory factory = new MicroserviceFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("CREATE - cria e consulta um microsserviço ativo")
    void criaMicrosservico() {
        var created = microservices.create(factory.valid()).expectCreated().expect("lifecycle", "ACTIVE").extractBody();
        microservices.findByIdentifier(created.identifier()).expectOk().expect("identifier", created.identifier()).expect("code", created.code());
    }

    @Test @DisplayName("VALIDATION - rejeita campos, formato, settings e versão inválidos")
    void rejeitaDadosInvalidos() {
        microservices.create(factory.withoutName()).expectBadRequest().expectContainsItem("details.field", "name");
        microservices.create(factory.withoutCode()).expectBadRequest();
        microservices.create(factory.withInvalidCode()).expectBadRequest();
        microservices.create(factory.withInvalidSettings()).expectBadRequest();
        microservices.create(factory.withVersion()).expectBadRequest().expectContainsItem("details.field", "version");
    }

    @Test @DisplayName("VALIDATION - rejeita código ou nome duplicado")
    void rejeitaDuplicidade() {
        var existing = factory.valid();
        microservices.create(existing).expectCreated();
        microservices.create(factory.withCode(existing.code())).expectBadRequest().expectContainsItem("details.field", "code");
        microservices.create(factory.withName(existing.name())).expectBadRequest().expectContainsItem("details.field", "name");
    }

    @Test @DisplayName("AUTH - exige autorização de owner para criar microsserviço")
    void exigeAutorizacaoDeOwner() {
        authorization.forbidden();
        microservices.create(factory.valid()).expectForbidden();
        authorization.verifyCalledWithPolicy("OWNER");
    }
}
