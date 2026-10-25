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

@DisplayName("Integração HTTP - Workspace Environment Configuration Inactivate")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class WorkspaceEnvironmentConfigurationInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private WorkspaceEnvironmentConfigurationScenario scenario;
    @Autowired private WorkspaceEnvironmentConfigurationClient configurations;
    private final WorkspaceEnvironmentConfigurationFactory factory = new WorkspaceEnvironmentConfigurationFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("LIFECYCLE - inativa a configuração e a remove da lista ativa")
    void inativaConfiguracao() {
        var created = configurations.create(scenario.workspaceIdentifier(), factory.valid(scenario.environmentIdentifier()))
                .expectCreated().extractBody();

        configurations.inactivate(scenario.workspaceIdentifier(), created.identifier()).expectNoContent();
        configurations.find(scenario.workspaceIdentifier(), created.identifier()).expectOk()
                .expect("lifecycle", "INACTIVE");
        configurations.findAll(scenario.workspaceIdentifier()).execute().expectOk().expectSize("content", 0);
    }

    @Test
    @DisplayName("NOT FOUND - rejeita inativação de configuração inexistente")
    void rejeitaConfiguracaoInexistente() {
        configurations.inactivate(scenario.workspaceIdentifier(), "missing-configuration").expectNotFound();
    }
}
