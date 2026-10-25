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

@DisplayName("Integração HTTP - schema binding find")
class SchemaBindingFindIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaBindingClient bindings;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();
    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("QUERY - busca binding persistido por identifier")
    void buscaBindingExistente() {
        String schema = scenario.platformDefaultSchemaIdentifier();
        var created = bindings.create(factory.validBinding("TEST_RESOURCE", "resource-find", schema))
                .expectCreated().extractBinding();
        bindings.find(created.identifier()).expectOk().expect("identifier", created.identifier())
                .expect("schemaIdentifier", schema);
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado para binding ausente")
    void retornaNaoEncontrado() { bindings.find("missing-binding").expectNotFound(); }

    @Test
    @DisplayName("AUTH - exige autorização para buscar binding")
    void exigeAutorizacao() {
        authorization.forbidden();
        bindings.find("missing-binding").expectForbidden();
    }
}
