package br.com.portalmanager.platform.workspace.integration.foundation.schema.schema;

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

@DisplayName("Integração HTTP - workspace schema inactivate")
class WorkspaceSchemaInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("INACTIVATE - inativa schema ativo do workspace")
    void inativaSchemaDoWorkspace() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse created = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();

        schemas.inactivate(workspace, created.identifier()).expectOk()
                .expect("lifecycle", "INACTIVE").expect("workspaceIdentifier", workspace);
    }

    @Test
    @DisplayName("VALIDATION - retorna não encontrado para schema ausente")
    void rejeitaSchemaAusente() {
        String workspace = scenario.createWorkspace().identifier();
        schemas.inactivate(workspace, "missing-schema").expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para inativar schema do workspace")
    void exigeAutorizacao() {
        String workspace = scenario.createWorkspace().identifier();
        authorization.forbidden();

        schemas.inactivate(workspace, "missing-schema").expectForbidden();
    }
}
