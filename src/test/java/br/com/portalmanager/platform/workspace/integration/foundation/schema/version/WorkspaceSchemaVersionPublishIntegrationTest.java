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

@DisplayName("Integração HTTP - workspace schema version publish")
class WorkspaceSchemaVersionPublishIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("PUBLISH - publica versões draft do schema do workspace")
    void publicaVersoesMonotonicamente() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse schema = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        SchemaVersionResponse first = schemas.findVersions(workspace, schema.identifier()).expectOk().extractVersions().getFirst();
        schemas.publish(workspace, schema.identifier(), first.identifier()).expectOk().expect("status", "PUBLISHED");
        SchemaVersionResponse second = schemas.createVersion(workspace, schema.identifier(), factory.validVersion("Workspace v2"))
                .expectCreated().extractVersion();

        schemas.publish(workspace, schema.identifier(), second.identifier()).expectOk()
                .expect("version", 2).expect("status", "PUBLISHED");
    }

    @Test
    @DisplayName("VALIDATION - retorna não encontrado para schema ou versão ausente")
    void rejeitaSchemaOuVersaoAusente() {
        String workspace = scenario.createWorkspace().identifier();
        schemas.publish(workspace, "missing-schema", "missing-version").expectNotFound();
        SchemaResponse schema = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();

        schemas.publish(workspace, schema.identifier(), "missing-version").expectNotFound();
    }

    @Test
    @DisplayName("AUTH - exige autorização para publicar versão do workspace")
    void exigeAutorizacao() {
        String workspace = scenario.createWorkspace().identifier();
        authorization.forbidden();

        schemas.publish(workspace, "missing-schema", "missing-version").expectForbidden();
    }
}
