package br.com.portalmanager.platform.workspace.integration.foundation.schema.version;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaIntegrationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - schema version create")
class SchemaVersionCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaClient schemas;
    @Autowired private SchemaIntegrationScenario scenario;
    private final SchemaFactory factory = new SchemaFactory();
    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test
    @DisplayName("CREATE - cria nova versão draft monotônica em schema platform e workspace")
    void criaVersaoDraftMonotonica() {
        var platform = schemas.create(factory.valid()).expectCreated().extractSchema();
        var first = schemas.findVersions(platform.identifier()).expectOk().extractVersions().getFirst();
        schemas.publish(platform.identifier(), first.identifier()).expectOk();
        var next = schemas.createVersion(platform.identifier(), factory.validVersion("Platform v2"))
                .expectCreated().extractVersion();
        schemas.findVersions(platform.identifier()).expectOk().expect("[0].version", 2)
                .expect("[0].status", "DRAFT");

        String workspace = scenario.createWorkspace().identifier();
        var scoped = schemas.create(workspace, factory.valid()).expectCreated().extractSchema();
        var initial = schemas.findVersions(workspace, scoped.identifier()).expectOk().extractVersions().getFirst();
        schemas.publish(workspace, scoped.identifier(), initial.identifier()).expectOk();
        schemas.createVersion(workspace, scoped.identifier(), factory.validVersion("Workspace v2"))
                .expectCreated().expect("version", 2).expect("status", "DRAFT");
    }

    @Test
    @DisplayName("VALIDATION - rejeita versão sem definição")
    void rejeitaVersaoSemDefinicao() {
        var schema = schemas.create(factory.valid()).expectCreated().extractSchema();
        schemas.createVersion(schema.identifier(), factory.withoutVersionDefinition())
                .expectBadRequest().expectContainsItem("details.field", "definition");
    }

    @Test
    @DisplayName("AUTH - exige autorização para criar versão")
    void exigeAutorizacao() {
        var schema = schemas.create(factory.valid()).expectCreated().extractSchema();
        authorization.forbidden();
        schemas.createVersion(schema.identifier(), factory.validVersion("Forbidden draft")).expectForbidden();
    }
}
