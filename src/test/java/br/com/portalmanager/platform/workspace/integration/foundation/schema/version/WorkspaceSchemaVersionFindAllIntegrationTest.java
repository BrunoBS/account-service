package br.com.portalmanager.platform.workspace.integration.foundation.schema.version;

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

@DisplayName("Integração HTTP - workspace schema version find all")
class WorkspaceSchemaVersionFindAllIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("QUERY - lista versões do schema do workspace em ordem decrescente")
    void listaVersoesMaisRecentesPrimeiro() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse schema = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        SchemaVersionResponse first = schemas.findVersions(workspace, schema.identifier()).expectOk().extractVersions().getFirst();
        schemas.publish(workspace, schema.identifier(), first.identifier()).expectOk();
        schemas.createVersion(workspace, schema.identifier(), factory.validVersion("Workspace v2")).expectCreated();

        schemas.findVersions(workspace, schema.identifier()).expectOk()
                .expect("[0].version", 2).expect("[1].version", 1);
    }

    @Test
    @DisplayName("QUERY - retorna não encontrado para schema ausente no workspace")
    void rejeitaSchemaAusente() {
        String workspace = scenario.createWorkspace().identifier();
        schemas.findVersions(workspace, "missing-schema").expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para listar versões do workspace")
    void exigeAutorizacao() {
        String workspace = scenario.createWorkspace().identifier();
        authorization.forbidden();

        schemas.findVersions(workspace, "missing-schema").expectForbidden();
    }
}
