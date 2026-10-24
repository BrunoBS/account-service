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

@DisplayName("Integração HTTP - Application Environment Configuration Find")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class ApplicationEnvironmentConfigurationFindIntegrationTest extends BaseIntegrationTest {
    @Autowired private ApplicationEnvironmentConfigurationScenario scenario;
    @Autowired private ApplicationEnvironmentConfigurationClient configurations;
    private final ApplicationEnvironmentConfigurationFactory factory = new ApplicationEnvironmentConfigurationFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("QUERY - busca a configuração e seus publishers pelo identificador")
    void buscaConfiguracao() {
        var created = configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(),
                factory.valid(scenario.environmentIdentifier())).expectCreated().extractBody();

        configurations.find(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), created.identifier())
                .expectOk().expect("identifier", created.identifier())
                .expect("environmentIdentifier", scenario.environmentIdentifier())
                .expect("publishers[0].publisherCode", "KAFKA");
    }

    @Test
    @DisplayName("NOT FOUND - oculta configuração inexistente")
    void rejeitaIdentificadorInexistente() {
        configurations.find(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), "missing-configuration")
                .expectNotFound();
    }

    @Test @DisplayName("AUTH - não expõe configuração de outra aplicação")
    void naoExpoeConfiguracaoDeOutraAplicacao() {
        var created = configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(),
                factory.valid(scenario.environmentIdentifier())).expectCreated().extractBody();
        String otherApplication = scenario.createAnotherApplication();

        configurations.find(scenario.workspaceIdentifier(), otherApplication, created.identifier())
                .expectNotFound();
    }
}
