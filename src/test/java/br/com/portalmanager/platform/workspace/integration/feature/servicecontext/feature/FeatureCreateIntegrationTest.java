package br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureClient;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureFactory;
import br.com.portalmanager.platform.workspace.integration.feature.servicecontext.feature.support.FeatureScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Service Context Feature Create")
class FeatureCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private FeatureScenario scenario;
    @Autowired private FeatureClient features;
    private final FeatureFactory factory = new FeatureFactory();
    @BeforeEach void prepare() { scenario.setup(); }

    @Test @DisplayName("CREATE - cria e consulta feature vinculada a microsserviço ativo")
    void criaFeature() {
        var created = features.create(factory.valid(scenario.microserviceIdentifier())).expectCreated().expect("lifecycle", "ACTIVE").extractBody();
        features.findByIdentifier(created.identifier()).expectOk().expect("identifier", created.identifier()).expect("microserviceIdentifier", scenario.microserviceIdentifier());
    }

    @Test @DisplayName("VALIDATION - rejeita dados obrigatórios, formato, settings e dependências inválidas")
    void rejeitaDadosInvalidos() {
        features.create(factory.withoutName(scenario.microserviceIdentifier())).expectBadRequest();
        features.create(factory.withoutCode(scenario.microserviceIdentifier())).expectBadRequest();
        features.create(factory.withoutSettings(scenario.microserviceIdentifier())).expectBadRequest();
        features.create(factory.withInvalidSettings(scenario.microserviceIdentifier())).expectBadRequest();
        features.create(factory.withInvalidCode(scenario.microserviceIdentifier())).expectBadRequest();
        features.create(factory.withVersion(scenario.microserviceIdentifier())).expectBadRequest().expectContainsItem("details.field", "version");
        features.create(factory.withoutMicroservice()).expectBadRequest();
        features.create(factory.withMicroservice("00000000-0000-0000-0000-000000000000")).expectNotFound();
    }

    @Test @DisplayName("VALIDATION - rejeita códigos e nomes duplicados")
    void rejeitaDuplicidade() {
        var created = factory.valid(scenario.microserviceIdentifier());
        features.create(created).expectCreated();
        features.create(factory.withCode(scenario.microserviceIdentifier(), created.code())).expectBadRequest().expectContainsItem("details.field", "code");
        features.create(factory.withName(scenario.microserviceIdentifier(), created.name())).expectBadRequest().expectContainsItem("details.field", "name");
    }

    @Test @DisplayName("AUTH - exige autorização de owner para criar feature")
    void exigeAutorizacaoDeOwner() {
        authorization.forbidden();
        features.create(factory.valid(scenario.microserviceIdentifier())).expectForbidden();
        authorization.verifyCalledWithPolicy("OWNER");
    }
}
