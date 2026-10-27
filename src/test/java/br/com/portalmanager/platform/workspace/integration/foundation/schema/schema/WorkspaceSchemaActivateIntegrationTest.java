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

@DisplayName("Integração HTTP - workspace schema activate")
class WorkspaceSchemaActivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("ACTIVATE - ativa schema inativo do workspace")
    void ativaSchemaDoWorkspace() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse created = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        schemas.inactivate(workspace, created.identifier()).expectOk();

        schemas.activate(workspace, created.identifier()).expectOk()
                .expect("lifecycle", "ACTIVE").expect("workspaceIdentifier", workspace);
    }

    @Test
    @DisplayName("VALIDATION - retorna não encontrado para schema ausente")
    void rejeitaSchemaAusente() {
        String workspace = scenario.createWorkspace().identifier();
        schemas.activate(workspace, "missing-schema").expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para ativar schema do workspace")
    void exigeAutorizacao() {
        String workspace = scenario.createWorkspace().identifier();
        authorization.forbidden();

        schemas.activate(workspace, "missing-schema").expectForbidden();
    }
}
