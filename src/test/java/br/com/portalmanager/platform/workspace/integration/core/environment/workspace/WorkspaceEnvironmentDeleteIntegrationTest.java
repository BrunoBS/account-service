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

@DisplayName("Integração HTTP - Workspace Environment Delete")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class WorkspaceEnvironmentDeleteIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private WorkspaceEnvironmentClient environments;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("LIFECYCLE - exige inativação antes da exclusão")
    void removeSomenteAmbienteInativo() {
        var created = environments.create(scenario.workspaceIdentifier(), factory.valid())
                .expectCreated()
                .extractBody();

        environments.delete(scenario.workspaceIdentifier(), created.identifier()).expectBadRequest();
        environments.inactivate(scenario.workspaceIdentifier(), created.identifier()).expectNoContent();
        environments.delete(scenario.workspaceIdentifier(), created.identifier()).expectNoContent();
    }

    @Test
    @DisplayName("NOT FOUND - rejeita exclusão de ambiente inexistente")
    void rejeitaExclusaoDeAmbienteInexistente() {
        environments.delete(scenario.workspaceIdentifier(), "missing-environment").expectNotFound();
    }
}
