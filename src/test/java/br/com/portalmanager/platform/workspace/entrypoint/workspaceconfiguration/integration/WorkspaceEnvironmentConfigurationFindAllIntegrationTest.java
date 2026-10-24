package br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.integration;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationClient;
import br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationScenario;
import br.com.portalmanager.platform.workspace.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Workspace Environment Configuration FindAll")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class WorkspaceEnvironmentConfigurationFindAllIntegrationTest extends BaseIntegrationTest {
    @Autowired private WorkspaceEnvironmentConfigurationScenario scenario;
    @Autowired private WorkspaceEnvironmentConfigurationClient configurations;
    private final WorkspaceEnvironmentConfigurationFactory factory = new WorkspaceEnvironmentConfigurationFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("QUERY - pagina configurações pelo ciclo de vida")
    void listaConfiguracoesPaginadasPorLifecycle() {
        var created = configurations.create(scenario.workspaceIdentifier(), factory.valid(scenario.environmentIdentifier()))
                .expectCreated().extractBody();
        configurations.inactivate(scenario.workspaceIdentifier(), created.identifier()).expectNoContent();

        configurations.findAll(scenario.workspaceIdentifier()).lifecycle("INACTIVE").page(0).size(1).execute()
                .expectOk().expectSize("content", 1)
                .expect("content[0].identifier", created.identifier())
                .expect("page.totalElements", 1);
        configurations.findAll(scenario.workspaceIdentifier()).execute().expectOk().expectSize("content", 0);
    }

    @Test
    @DisplayName("VALIDATION - rejeita lifecycle e ordenação inválidos")
    void rejeitaParametrosInvalidos() {
        configurations.findAll(scenario.workspaceIdentifier()).lifecycle("UNKNOWN").execute().expectBadRequest();
        configurations.findAll(scenario.workspaceIdentifier()).sort("settings").execute().expectBadRequest();
    }
}
