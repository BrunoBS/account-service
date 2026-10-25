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

@DisplayName("Integração HTTP - Workspace Environment Configuration Find")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class WorkspaceEnvironmentConfigurationFindIntegrationTest extends BaseIntegrationTest {
    @Autowired private WorkspaceEnvironmentConfigurationScenario scenario;
    @Autowired private WorkspaceEnvironmentConfigurationClient configurations;
    private final WorkspaceEnvironmentConfigurationFactory factory = new WorkspaceEnvironmentConfigurationFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("QUERY - encontra a configuração e seus publishers pelo identificador")
    void buscaConfiguracao() {
        var created = configurations.create(scenario.workspaceIdentifier(), factory.valid(scenario.environmentIdentifier()))
                .expectCreated().extractBody();

        configurations.find(scenario.workspaceIdentifier(), created.identifier()).expectOk()
                .expect("identifier", created.identifier())
                .expect("environmentIdentifier", scenario.environmentIdentifier())
                .expect("publishers[0].publisherCode", "KAFKA")
                .expect("publishers[0].parameters.topicName", "test-topic");
    }

    @Test
    @DisplayName("NOT FOUND - rejeita configuração de outro identificador")
    void rejeitaConfiguracaoInexistente() {
        configurations.find(scenario.workspaceIdentifier(), "missing-configuration").expectNotFound();
    }

    @Test
    @DisplayName("AUTH - não expõe configuração de outro workspace")
    void naoExpoeConfiguracaoDeOutroWorkspace() {
        var created = configurations.create(scenario.workspaceIdentifier(), factory.valid(scenario.environmentIdentifier()))
                .expectCreated().extractBody();
        String otherWorkspace = scenario.createAnotherWorkspace();

        configurations.find(otherWorkspace, created.identifier()).expectNotFound();
    }
}
