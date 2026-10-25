package br.com.portalmanager.platform.workspace.integration.foundation.schema.binding;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaBindingClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaIntegrationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - schema binding activate")
class SchemaBindingActivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaBindingClient bindings;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();
    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("ACTIVATE - reativa binding inativo")
    void reativaBindingInativo() {
        var created = bindings.create(factory.validBinding("TEST_RESOURCE", "resource-activate",
                scenario.platformDefaultSchemaIdentifier())).expectCreated().extractBinding();
        bindings.inactivate(created.identifier()).expectOk();
        bindings.activate(created.identifier()).expectOk().expect("lifecycle", "ACTIVE");
    }

    @Test
    @DisplayName("VALIDATION - retorna não encontrado para binding ausente")
    void rejeitaBindingAusente() { bindings.activate("missing-binding").expectNotFound(); }

    @Test
    @DisplayName("AUTH - exige autorização para ativar binding")
    void exigeAutorizacao() {
        authorization.forbidden();
        bindings.activate("missing-binding").expectForbidden();
    }
}
