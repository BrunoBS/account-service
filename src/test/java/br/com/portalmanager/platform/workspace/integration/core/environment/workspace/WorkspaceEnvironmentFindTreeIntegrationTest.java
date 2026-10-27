package br.com.portalmanager.platform.workspace.integration.core.environment.workspace;

import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentFactory;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentUpdateBuilder;
import br.com.portalmanager.platform.workspace.integration.core.environment.workspace.support.WorkspaceEnvironmentClient;
import br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Workspace Environment Tree")
@WithMySql(excludeTables = {"flyway_schema_history"})
class WorkspaceEnvironmentFindTreeIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private WorkspaceEnvironmentClient environments;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("QUERY - retorna a árvore completa de ambientes")
    void consultaArvore() {
        var root = environments.create(scenario.workspaceIdentifier(), factory.valid("Root environment"))
                .expectCreated()
                .extractBody();
        environments.create(scenario.workspaceIdentifier(), factory.shard("Child environment", root.identifier()))
                .expectCreated();

        environments.tree(scenario.workspaceIdentifier())
                .expectOk()
                .expectSize("", 1)
                .expect("[0].environment.identifier", root.identifier())
                .expectSize("[0].children", 1);
    }
}
