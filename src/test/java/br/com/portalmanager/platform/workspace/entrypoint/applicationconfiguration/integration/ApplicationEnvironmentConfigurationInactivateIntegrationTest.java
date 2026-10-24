package br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.integration;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.support.ApplicationEnvironmentConfigurationClient;
import br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.support.ApplicationEnvironmentConfigurationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.support.ApplicationEnvironmentConfigurationScenario;
import br.com.portalmanager.platform.workspace.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Application Environment Configuration Inactivate")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class ApplicationEnvironmentConfigurationInactivateIntegrationTest extends BaseIntegrationTest {
    @Autowired private ApplicationEnvironmentConfigurationScenario scenario;
    @Autowired private ApplicationEnvironmentConfigurationClient configurations;
    private final ApplicationEnvironmentConfigurationFactory factory = new ApplicationEnvironmentConfigurationFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("LIFECYCLE - inativa configuração e remove-a da lista ativa")
    void inativaConfiguracao() {
        var created = configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(),
                factory.valid(scenario.environmentIdentifier())).expectCreated().extractBody();

        configurations.inactivate(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), created.identifier())
                .expectNoContent();
        configurations.find(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), created.identifier())
                .expectOk().expect("lifecycle", "INACTIVE");
        configurations.findAll(scenario.workspaceIdentifier(), scenario.applicationIdentifier())
                .execute().expectOk().expectSize("content", 0);
    }

    @Test
    @DisplayName("NOT FOUND - rejeita inativação de configuração inexistente")
    void rejeitaConfiguracaoInexistente() {
        configurations.inactivate(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), "missing-configuration")
                .expectNotFound();
    }
}
