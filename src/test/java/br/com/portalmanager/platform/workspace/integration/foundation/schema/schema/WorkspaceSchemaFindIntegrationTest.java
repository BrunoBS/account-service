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

@DisplayName("Integração HTTP - workspace schema find")
class WorkspaceSchemaFindIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("QUERY - busca schema pelo identificador dentro do workspace")
    void buscaSchemaDoWorkspace() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse created = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();

        schemas.findByIdentifier(workspace, created.identifier()).expectOk()
                .expect("identifier", created.identifier()).expect("workspaceIdentifier", workspace);
    }

    @Test
    @DisplayName("QUERY - não expõe schema global pelo caminho de workspace")
    void naoEncontraSchemaDeOutroEscopo() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse platform = schemas.create(factory.valid()).expectCreated().extractSchema();

        schemas.findByIdentifier(workspace, platform.identifier()).expectNotFound();
        schemas.findByIdentifier(workspace, "missing-schema").expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para buscar schema no workspace")
    void exigeAutorizacao() {
        String workspace = scenario.createWorkspace().identifier();
        authorization.forbidden();

        schemas.findByIdentifier(workspace, "missing-schema").expectForbidden();
    }
}
