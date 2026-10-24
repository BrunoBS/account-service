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

@DisplayName("Integração HTTP - Workspace Environment Restore")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class WorkspaceEnvironmentRestoreIntegrationTest extends BaseIntegrationTest {
    @Autowired private EnvironmentScenario scenario;
    @Autowired private WorkspaceEnvironmentClient environments;
    private final EnvironmentFactory factory = new EnvironmentFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("LIFECYCLE - restaura ambiente inativo")
    void restauraAmbiente() {
        var created = environments.create(scenario.workspaceIdentifier(), factory.valid())
                .expectCreated()
                .extractBody();
        environments.inactivate(scenario.workspaceIdentifier(), created.identifier()).expectNoContent();

        environments.restore(scenario.workspaceIdentifier(), created.identifier())
                .expectOk()
                .expect("lifecycle", "ACTIVE");
    }

    @Test
    @DisplayName("NOT FOUND - rejeita restauração de ambiente inexistente")
    void rejeitaRestauracaoDeAmbienteInexistente() {
        environments.restore(scenario.workspaceIdentifier(), "missing-environment").expectNotFound();
    }
}
