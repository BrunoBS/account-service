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

@DisplayName("Integração HTTP - schema binding inactivate")
class SchemaBindingInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaBindingClient bindings;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();
    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("INACTIVATE - inativa binding e mantém a alteração consultável")
    void inativaBinding() {
        var created = bindings.create(factory.validBinding("TEST_RESOURCE", "resource-inactivate",
                scenario.platformDefaultSchemaIdentifier())).expectCreated().extractBinding();
        bindings.inactivate(created.identifier()).expectOk().expect("lifecycle", "INACTIVE");
        bindings.find(created.identifier()).expectOk().expect("lifecycle", "INACTIVE");
    }

    @Test
    @DisplayName("VALIDATION - retorna não encontrado para binding ausente")
    void rejeitaBindingAusente() { bindings.inactivate("missing-binding").expectNotFound(); }

    @Test
    @DisplayName("AUTH - exige autorização para inativar binding")
    void exigeAutorizacao() {
        authorization.forbidden();
        bindings.inactivate("missing-binding").expectForbidden();
    }
}
