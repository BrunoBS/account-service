package br.com.portalmanager.platform.workspace.integration.foundation.schema.schema;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaIntegrationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - schema quarantine")
class SchemaDeleteIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();
    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("DELETE - coloca schemas de plataforma e workspace em quarentena")
    void colocaSchemasEmQuarentena() {
        var platform = schemas.create(factory.valid()).expectCreated().extractSchema();
        schemas.quarantine(platform.identifier()).expectNoContent();
        assertTrue(schemas.findAll("QUARANTINED").expectOk().extractSchemas().stream()
                .anyMatch(schema -> platform.identifier().equals(schema.identifier())));

        String workspace = scenario.createWorkspace().identifier();
        var scoped = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        schemas.quarantine(workspace, scoped.identifier()).expectNoContent();
        assertTrue(schemas.findAll(workspace, "QUARANTINED").expectOk().extractSchemas().stream()
                .anyMatch(schema -> scoped.identifier().equals(schema.identifier())));
    }

    @Test
    @DisplayName("VALIDATION - retorna não encontrado ao colocar schema ausente em quarentena")
    void rejeitaSchemaAusente() { schemas.quarantine("missing-schema").expectNotFound(); }

    @Test
    @DisplayName("AUTH - exige autorização para colocar schema em quarentena")
    void exigeAutorizacao() {
        authorization.forbidden();
        schemas.quarantine("missing-schema").expectForbidden();
    }
}
