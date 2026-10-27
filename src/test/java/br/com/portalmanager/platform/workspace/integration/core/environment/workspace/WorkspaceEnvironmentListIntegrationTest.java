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

@DisplayName("Integração HTTP - Workspace Environment List")
@WithMySql(excludeTables = {"flyway_schema_history"})
class WorkspaceEnvironmentListIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private WorkspaceEnvironmentClient environments;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("QUERY - filtra ambientes pelo ciclo de vida")
    void listaPorCicloDeVida() {
        var created = environments.create(scenario.workspaceIdentifier(), factory.valid())
                .expectCreated()
                .extractBody();

        environments.list(scenario.workspaceIdentifier(), "ACTIVE").expectOk().expectSize("", 1);
        environments.inactivate(scenario.workspaceIdentifier(), created.identifier()).expectNoContent();
        environments.list(scenario.workspaceIdentifier(), "INACTIVE").expectOk().expectSize("", 1);
    }

    @Test
    @DisplayName("VISIBILITY - oculta filhos ativos quando um ancestral é inativado")
    void ocultaFilhosDeAncestralInativo() {
        var root = environments.create(scenario.workspaceIdentifier(), factory.valid("Root environment"))
                .expectCreated()
                .extractBody();
        var child = environments.create(scenario.workspaceIdentifier(), factory.shard("Child environment", root.identifier()))
                .expectCreated()
                .extractBody();

        environments.inactivate(scenario.workspaceIdentifier(), root.identifier()).expectNoContent();

        environments.list(scenario.workspaceIdentifier(), "ACTIVE")
                .expectOk()
                .expectSize("", 0);
        environments.list(scenario.workspaceIdentifier(), "INACTIVE")
                .expectOk()
                .expectSize("", 1)
                .expect("[0].identifier", root.identifier());
        environments.find(scenario.workspaceIdentifier(), child.identifier()).expectNotFound();
    }
}
