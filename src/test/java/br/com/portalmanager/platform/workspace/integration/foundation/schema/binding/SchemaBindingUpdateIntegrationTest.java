package br.com.portalmanager.platform.workspace.integration.foundation.schema.binding;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaBindingClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaIntegrationScenario;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaClient;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - schema binding update")
class SchemaBindingUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaBindingClient bindings;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();
    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("UPDATE - troca o schema associado e persiste a nova referência")
    void atualizaSchemaAssociado() {
        String originalSchema = scenario.platformDefaultSchemaIdentifier();
        var replacement = schemas.create(factory.valid()).expectCreated().extractSchema();
        var binding = bindings.create(factory.validBinding("TEST_RESOURCE", "resource-update", originalSchema))
                .expectCreated().extractBinding();
        bindings.update(binding.identifier(), factory.updateBinding(binding.version(), replacement.identifier()))
                .expectOk().expect("schemaIdentifier", replacement.identifier());
        bindings.find(binding.identifier()).expectOk().expect("schemaIdentifier", replacement.identifier());
    }

    @Test
    @DisplayName("VALIDATION - rejeita versão desatualizada, schema inexistente e binding ausente")
    void rejeitaAtualizacaoInvalida() {
        String original = scenario.platformDefaultSchemaIdentifier();
        var replacement = schemas.create(factory.valid()).expectCreated().extractSchema();
        var binding = bindings.create(factory.validBinding("TEST_RESOURCE", "resource-stale", original))
                .expectCreated().extractBinding();
        bindings.update(binding.identifier(), factory.updateBinding(binding.version(), replacement.identifier()))
                .expectOk();
        bindings.update(binding.identifier(), factory.staleBinding(binding.version(), original)).expectConflict();
        bindings.update(binding.identifier(), factory.updateBinding(1L, "missing-schema")).expectNotFound();
        bindings.update("missing-binding", factory.updateBinding(0L, original)).expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para atualizar binding")
    void exigeAutorizacao() {
        authorization.forbidden();
        bindings.update("missing-binding", factory.updateBinding(0L, "missing-schema")).expectForbidden();
    }
}
