package br.com.portalmanager.platform.workspace.integration.foundation.schema.schema;

import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.entrypoint.schema.response.SchemaResponse;
import br.com.portalmanager.platform.workspace.entrypoint.schema.response.SchemaVersionResponse;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaIntegrationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - workspace schema delete")
class WorkspaceSchemaDeleteIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("DELETE - coloca schema do workspace em quarentena")
    void colocaSchemaEmQuarentena() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse created = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();

        schemas.quarantine(workspace, created.identifier()).expectNoContent();
        assertTrue(schemas.findAll(workspace, "QUARANTINED").expectOk().extractSchemas().stream()
                .anyMatch(schema -> created.identifier().equals(schema.identifier())));
    }

    @Test
    @DisplayName("VALIDATION - retorna não encontrado ao excluir schema ausente")
    void rejeitaSchemaAusente() {
        String workspace = scenario.createWorkspace().identifier();
        schemas.quarantine(workspace, "missing-schema").expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para excluir schema do workspace")
    void exigeAutorizacao() {
        String workspace = scenario.createWorkspace().identifier();
        authorization.forbidden();

        schemas.quarantine(workspace, "missing-schema").expectForbidden();
    }
}
