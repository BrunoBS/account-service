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
    @Autowired private jakarta.persistence.EntityManagerFactory entityManagerFactory;
    @Autowired private br.com.portalmanager.platform.workspace.integration.core.environment.workspace.support.WorkspaceEnvironmentClient workspaceEnvironments;
    @Autowired private br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationClient workspaceConfigurations;

    @Test
    @DisplayName("REGRESSION FIN-PERF-001 - orçamento SQL não cresce por configuração listada")
    void consultasDeReferenciasEPublishersSaoEmLote() {
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), factory.valid(scenario.environmentIdentifier())).expectCreated();
        org.hibernate.stat.Statistics statistics = entityManagerFactory.unwrap(org.hibernate.SessionFactory.class).getStatistics();
        boolean previous = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        try {
            statistics.clear();
            configurations.findAll(scenario.workspaceIdentifier(), scenario.applicationIdentifier()).execute().expectOk().expectSize("content", 1);
            long one = statistics.getPrepareStatementCount();
            for (int index = 0; index < 2; index++) {
                String environment = scenario.createEnvironmentWithoutWorkspaceConfiguration();
            workspaceConfigurations.create(scenario.workspaceIdentifier(),
                    new br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationFactory().valid(environment)).expectCreated();
                configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), factory.valid(environment)).expectCreated();
            }
            statistics.clear();
            configurations.findAll(scenario.workspaceIdentifier(), scenario.applicationIdentifier()).execute().expectOk().expectSize("content", 3);
            long three = statistics.getPrepareStatementCount();
            org.assertj.core.api.Assertions.assertThat(three).isEqualTo(one);
            org.assertj.core.api.Assertions.assertThat(three).isLessThanOrEqualTo(6);
        } finally { statistics.setStatisticsEnabled(previous); }
    }
}
