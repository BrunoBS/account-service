package br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationClient;
import br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationFactory;
import br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Workspace Environment Configuration Delete")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class WorkspaceEnvironmentConfigurationDeleteIntegrationTest extends BaseIntegrationTest {
    @Autowired private WorkspaceEnvironmentConfigurationScenario scenario;
    @Autowired private WorkspaceEnvironmentConfigurationClient configurations;
    private final WorkspaceEnvironmentConfigurationFactory factory = new WorkspaceEnvironmentConfigurationFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("LIFECYCLE - coloca em quarentena somente configuração inativa")
    void excluiConfiguracaoInativa() {
        var created = configurations.create(scenario.workspaceIdentifier(), factory.valid(scenario.environmentIdentifier()))
                .expectCreated().extractBody();

        configurations.delete(scenario.workspaceIdentifier(), created.identifier()).expectBadRequest();
        configurations.inactivate(scenario.workspaceIdentifier(), created.identifier()).expectNoContent();
        configurations.delete(scenario.workspaceIdentifier(), created.identifier()).expectNoContent();
        configurations.find(scenario.workspaceIdentifier(), created.identifier()).expectOk()
                .expect("lifecycle", "QUARANTINED");
        configurations.findAll(scenario.workspaceIdentifier()).lifecycle("QUARANTINED").execute()
                .expectOk().expectSize("content", 1)
                .expect("content[0].identifier", created.identifier());
    }

    @Test
    @DisplayName("NOT FOUND - rejeita exclusão de configuração inexistente")
    void rejeitaConfiguracaoInexistente() {
        configurations.delete(scenario.workspaceIdentifier(), "missing-configuration").expectNotFound();
    }
}
