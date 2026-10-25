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

@DisplayName("Integração HTTP - schema binding delete")
class SchemaBindingDeleteIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaBindingClient bindings;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();
    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("DELETE - permite excluir binding inativo e confirma remoção")
    void excluiBindingInativo() {
        var created = bindings.create(factory.validBinding("TEST_RESOURCE", "resource-delete",
                scenario.platformDefaultSchemaIdentifier())).expectCreated().extractBinding();
        bindings.delete(created.identifier()).expectConflict();
        bindings.inactivate(created.identifier()).expectOk();
        bindings.delete(created.identifier()).expectNoContent();
        bindings.find(created.identifier()).expectNotFound();
    }

    @Test
    @DisplayName("VALIDATION - rejeita exclusão de binding ausente")
    void rejeitaBindingAusente() { bindings.delete("missing-binding").expectNotFound(); }

    @Test
    @DisplayName("AUTH - exige autorização para excluir binding")
    void exigeAutorizacao() {
        authorization.forbidden();
        bindings.delete("missing-binding").expectForbidden();
    }
}
