package br.com.portalmanager.platform.workspace.entrypoint.environment.workspace.integration;

import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentFactory;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentUpdateBuilder;
import br.com.portalmanager.platform.workspace.entrypoint.environment.workspace.support.WorkspaceEnvironmentClient;
import br.com.portalmanager.platform.workspace.entrypoint.environment.defaults.support.DefaultEnvironmentClient;
import br.com.portalmanager.platform.workspace.entrypoint.environment.support.EnvironmentScenario;
import br.com.portalmanager.platform.workspace.support.BaseIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Workspace Environment Roots")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class WorkspaceEnvironmentFindRootsIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private WorkspaceEnvironmentClient environments;
    @Autowired private DefaultEnvironmentClient defaults;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("QUERY - retorna somente ambientes raiz")
    void listaRaizes() {
        var defaultRoot = defaults.create(factory.validDefault("Global root environment"))
                .expectCreated()
                .extractBody();
        var root = environments.create(scenario.workspaceIdentifier(), factory.valid("Root environment"))
                .expectCreated()
                .extractBody();
        environments.create(scenario.workspaceIdentifier(), factory.shard("Child environment", root.identifier()))
                .expectCreated();

        environments.roots(scenario.workspaceIdentifier())
                .expectOk()
                .expectSize("", 2)
                .expect("[0].identifier", defaultRoot.identifier())
                .expect("[1].identifier", root.identifier());
    }
}
