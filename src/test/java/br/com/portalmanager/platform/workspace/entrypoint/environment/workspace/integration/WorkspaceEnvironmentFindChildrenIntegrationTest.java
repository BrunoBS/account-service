package br.com.portalmanager.platform.workspace.entrypoint.environment.workspace.integration;

import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentFactory;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentUpdateBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.environment.workspace.support.WorkspaceEnvironmentClient;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Workspace Environment Children")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class WorkspaceEnvironmentFindChildrenIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private WorkspaceEnvironmentClient environments;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("QUERY - retorna os filhos do ambiente informado")
    void listaFilhos() {
        var root = environments.create(scenario.workspaceIdentifier(), factory.valid("Root environment"))
                .expectCreated()
                .extractBody();
        var child = environments.create(scenario.workspaceIdentifier(), factory.shard("Child environment", root.identifier()))
                .expectCreated()
                .extractBody();

        environments.children(scenario.workspaceIdentifier(), root.identifier())
                .expectOk()
                .expectSize("", 1)
                .expect("[0].identifier", child.identifier());
    }
}
