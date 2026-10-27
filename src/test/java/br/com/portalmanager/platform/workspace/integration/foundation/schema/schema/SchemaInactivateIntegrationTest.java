package br.com.portalmanager.platform.workspace.integration.foundation.schema.schema;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaIntegrationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - schema inactivate")
class SchemaInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();
    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("INACTIVATE - inativa schema ativo de plataforma")
    void inativaSchemaDePlataforma() {
        var platform = schemas.create(factory.valid()).expectCreated().extractSchema();
        schemas.inactivate(platform.identifier()).expectOk().expect("lifecycle", "INACTIVE");

    }

    @Test
    @DisplayName("VALIDATION - retorna não encontrado ao inativar schema ausente")
    void rejeitaSchemaAusente() { schemas.inactivate("missing-schema").expectNotFound(); }

    @Test
    @DisplayName("AUTH - exige autorização para inativar schema")
    void exigeAutorizacao() {
        authorization.forbidden();
        schemas.inactivate("missing-schema").expectForbidden();
    }
}
