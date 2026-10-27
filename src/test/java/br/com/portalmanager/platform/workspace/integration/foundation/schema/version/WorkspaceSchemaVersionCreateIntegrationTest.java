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

@DisplayName("Integração HTTP - workspace schema version create")
class WorkspaceSchemaVersionCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("CREATE - cria versão draft monotônica no schema do workspace")
    void criaVersaoDraftNoWorkspace() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse schema = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        SchemaVersionResponse first = schemas.findVersions(workspace, schema.identifier()).expectOk().extractVersions().getFirst();
        schemas.publish(workspace, schema.identifier(), first.identifier()).expectOk();

        schemas.createVersion(workspace, schema.identifier(), factory.validVersion("Workspace v2"))
                .expectCreated().expect("version", 2).expect("status", "DRAFT");
    }

    @Test
    @DisplayName("VALIDATION - rejeita versão sem definição")
    void rejeitaVersaoSemDefinicao() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse schema = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();

        schemas.createVersion(workspace, schema.identifier(), factory.withoutVersionDefinition())
                .expectBadRequest().expectContainsItem("details.field", "definition");
    }

    @Test
    @DisplayName("AUTH - exige autorização para criar versão no workspace")
    void exigeAutorizacao() {
        String workspace = scenario.createWorkspace().identifier();
        SchemaResponse schema = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        authorization.forbidden();

        schemas.createVersion(workspace, schema.identifier(), factory.validVersion("Forbidden draft"))
                .expectForbidden();
    }
}
