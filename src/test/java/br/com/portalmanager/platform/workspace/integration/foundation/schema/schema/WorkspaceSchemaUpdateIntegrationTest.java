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

@DisplayName("Integração HTTP - workspace schema update")
class WorkspaceSchemaUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("UPDATE - atualiza metadados do schema no workspace")
    void atualizaSchemaDoWorkspace() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse created = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();

        schemas.update(workspace, created.identifier(), factory.update(created.version())).expectOk()
                .expect("workspaceIdentifier", workspace).expect("name", "Updated schema name");
        schemas.findByIdentifier(workspace, created.identifier()).expectOk()
                .expect("description", "Updated schema description");
    }

    @Test
    @DisplayName("VALIDATION - rejeita versão desatualizada e schema inexistente no workspace")
    void rejeitaVersaoDesatualizadaOuSchemaAusente() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse created = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        schemas.update(workspace, created.identifier(), factory.update(created.version())).expectOk();

        schemas.update(workspace, created.identifier(), factory.withStaleVersion(created.version())).expectConflict();
        schemas.update(workspace, "missing-schema", factory.update(0L)).expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para atualizar schema do workspace")
    void exigeAutorizacao() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse created = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        authorization.forbidden();

        schemas.update(workspace, created.identifier(), factory.update(created.version())).expectForbidden();
    }
}
