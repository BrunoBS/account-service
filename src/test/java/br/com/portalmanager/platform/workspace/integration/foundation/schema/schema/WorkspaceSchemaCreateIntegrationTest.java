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

@DisplayName("Integração HTTP - workspace schema create")
class WorkspaceSchemaCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("CREATE - cria schema no escopo do workspace")
    void criaSchemaNoWorkspace() {
        String workspace = scenario.createWorkspace().identifier();

        schemas.create(workspace, factory.valid()).expectCreated()
                .expect("scope", "WORKSPACE").expect("workspaceIdentifier", workspace);
    }

    @Test
    @DisplayName("VALIDATION - rejeita workspace inexistente ao criar schema")
    void rejeitaWorkspaceInexistente() {
        schemas.create("missing-workspace", factory.valid()).expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para criar schema no workspace")
    void exigeAutorizacao() {
        String workspace = scenario.createWorkspace().identifier();
        authorization.forbidden();

        schemas.create(workspace, factory.valid()).expectForbidden();
    }
}
