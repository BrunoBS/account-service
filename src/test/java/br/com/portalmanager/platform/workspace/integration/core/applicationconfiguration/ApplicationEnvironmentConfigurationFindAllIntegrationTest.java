package br.com.portalmanager.platform.workspace.integration.core.applicationconfiguration;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.integration.core.applicationconfiguration.support.ApplicationEnvironmentConfigurationClient;
import br.com.portalmanager.platform.workspace.integration.core.applicationconfiguration.support.ApplicationEnvironmentConfigurationFactory;
import br.com.portalmanager.platform.workspace.integration.core.applicationconfiguration.support.ApplicationEnvironmentConfigurationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Application Environment Configuration FindAll")
@WithMySql(excludeTables = {"flyway_schema_history"})
class ApplicationEnvironmentConfigurationFindAllIntegrationTest extends BaseIntegrationTest {
    @Autowired private ApplicationEnvironmentConfigurationScenario scenario;
    @Autowired private ApplicationEnvironmentConfigurationClient configurations;
    private final ApplicationEnvironmentConfigurationFactory factory = new ApplicationEnvironmentConfigurationFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("QUERY - pagina configurações e filtra pelo ciclo de vida")
    void listaConfiguracoesPaginadasPorLifecycle() {
        var created = configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(),
                factory.valid(scenario.environmentIdentifier())).expectCreated().extractBody();
        configurations.inactivate(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), created.identifier())
                .expectNoContent();

        configurations.findAll(scenario.workspaceIdentifier(), scenario.applicationIdentifier())
                .lifecycle("INACTIVE").page(0).size(1).execute()
                .expectOk().expectSize("content", 1).expect("content[0].identifier", created.identifier())
                .expect("page.totalElements", 1);
        configurations.findAll(scenario.workspaceIdentifier(), scenario.applicationIdentifier())
                .execute().expectOk().expectSize("content", 0);
    }

    @Test
    @DisplayName("VALIDATION - rejeita lifecycle e ordenação inválidos")
    void rejeitaParametrosInvalidos() {
        configurations.findAll(scenario.workspaceIdentifier(), scenario.applicationIdentifier())
                .lifecycle("UNKNOWN").execute().expectBadRequest();
        configurations.findAll(scenario.workspaceIdentifier(), scenario.applicationIdentifier())
                .sort("settings").execute().expectBadRequest();
    }
}
