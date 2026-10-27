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

@DisplayName("Integração HTTP - Workspace Environment Configuration Update")
@WithMySql(excludeTables = {"flyway_schema_history"})
class WorkspaceEnvironmentConfigurationUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private WorkspaceEnvironmentConfigurationScenario scenario;
    @Autowired private WorkspaceEnvironmentConfigurationClient configurations;
    private final WorkspaceEnvironmentConfigurationFactory factory = new WorkspaceEnvironmentConfigurationFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("UPDATE - atualiza settings e parâmetros do publisher com a versão atual")
    void atualizaSettingsEParameters() {
        var created = configurations.create(scenario.workspaceIdentifier(), factory.valid(scenario.environmentIdentifier()))
                .expectCreated().extractBody();

        configurations.update(scenario.workspaceIdentifier(), created.identifier(), factory.update(created.version()))
                .expectOk()
                .expect("version", created.version().intValue() + 1)
                .expect("settings.region", "updated")
                .expect("publishers[0].parameters.topicName", "updated-topic");
    }

    @Test
    @DisplayName("VALIDATION - rejeita versão, settings e publishers inválidos")
    void rejeitaPayloadInvalido() {
        var created = configurations.create(scenario.workspaceIdentifier(), factory.valid(scenario.environmentIdentifier()))
                .expectCreated().extractBody();

        configurations.update(scenario.workspaceIdentifier(), created.identifier(), factory.updateWithoutVersion())
                .expectBadRequest().expectContainsItem("details.field", "environmentIdentifier");
        configurations.update(scenario.workspaceIdentifier(), created.identifier(),
                        factory.updateWithInvalidSettings(created.version()))
                .expectBadRequest();
        configurations.update(scenario.workspaceIdentifier(), created.identifier(),
                        factory.updateWithoutPublishers(created.version()))
                .expectBadRequest();
    }

    @Test
    @DisplayName("CONCURRENCY - rejeita atualização com versão desatualizada")
    void rejeitaVersaoDesatualizada() {
        var created = configurations.create(scenario.workspaceIdentifier(), factory.valid(scenario.environmentIdentifier()))
                .expectCreated().extractBody();

        configurations.update(scenario.workspaceIdentifier(), created.identifier(), factory.update(created.version() + 1))
                .expectConflict();
    }

    @Test
    @DisplayName("LIFECYCLE - não permite atualizar configuração inativa")
    void naoAtualizaConfiguracaoInativa() {
        var created = configurations.create(scenario.workspaceIdentifier(), factory.valid(scenario.environmentIdentifier()))
                .expectCreated().extractBody();
        configurations.inactivate(scenario.workspaceIdentifier(), created.identifier()).expectNoContent();

        configurations.update(scenario.workspaceIdentifier(), created.identifier(), factory.update(created.version() + 1))
                .expectBadRequest();
    }
}
