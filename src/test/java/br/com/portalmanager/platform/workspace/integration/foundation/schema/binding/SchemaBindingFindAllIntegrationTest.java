package br.com.portalmanager.platform.workspace.integration.foundation.schema.binding;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaBindingClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaIntegrationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - schema binding find all")
class SchemaBindingFindAllIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaBindingClient bindings;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();
    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("QUERY - lista bindings por lifecycle")
    void filtraBindingsPorLifecycle() {
        String schema = scenario.platformDefaultSchemaIdentifier();
        var created = bindings.create(factory.validBinding("TEST_RESOURCE", "resource-list", schema))
                .expectCreated().extractBinding();
        bindings.inactivate(created.identifier()).expectOk();

        assertTrue(bindings.findAll("INACTIVE").expectOk().extractBindings().stream()
                .anyMatch(binding -> created.identifier().equals(binding.identifier())));
        assertTrue(bindings.findAll("ACTIVE").expectOk().extractBindings().stream()
                .noneMatch(binding -> created.identifier().equals(binding.identifier())));
    }

    @Test
    @DisplayName("VALIDATION - rejeita filtro de lifecycle desconhecido")
    void rejeitaLifecycleDesconhecido() { bindings.findAll("UNKNOWN").expectBadRequest(); }

    @Test
    @DisplayName("AUTH - exige autorização para listar bindings")
    void exigeAutorizacao() {
        authorization.forbidden();
        bindings.findAll().expectForbidden();
    }
}
