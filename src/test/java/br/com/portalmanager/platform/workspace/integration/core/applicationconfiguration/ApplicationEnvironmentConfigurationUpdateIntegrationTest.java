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

@DisplayName("Integração HTTP - Application Environment Configuration Update")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class ApplicationEnvironmentConfigurationUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired private ApplicationEnvironmentConfigurationScenario scenario;
    @Autowired private ApplicationEnvironmentConfigurationClient configurations;
    private final ApplicationEnvironmentConfigurationFactory factory = new ApplicationEnvironmentConfigurationFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("UPDATE - atualiza settings e substitui a lista de publishers")
    void atualizaSettingsEPublishers() {
        var created = configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(),
                factory.valid(scenario.environmentIdentifier())).expectCreated().extractBody();

        configurations.update(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), created.identifier(),
                factory.update(created.version())).expectOk()
                .expect("version", created.version().intValue() + 1)
                .expect("settings.region", "updated")
                .expect("publishers[0].publisherCode", "KAFKA");
    }

    @Test
    @DisplayName("VALIDATION - rejeita versão ausente ou settings inválidos")
    void rejeitaPayloadInvalido() {
        var created = configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(),
                factory.valid(scenario.environmentIdentifier())).expectCreated().extractBody();

        configurations.update(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), created.identifier(),
                        factory.updateWithoutVersion())
                .expectBadRequest().expectContainsItem("details.field", "environmentIdentifier");
        configurations.update(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), created.identifier(),
                        factory.updateWithInvalidSettings(created.version()))
                .expectBadRequest();
        configurations.update(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), created.identifier(),
                        factory.updateWithoutPublishers(created.version()))
                .expectBadRequest();
    }

    @Test
    @DisplayName("CONCURRENCY - rejeita atualização com versão desatualizada")
    void rejeitaVersaoDesatualizada() {
        var created = configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(),
                factory.valid(scenario.environmentIdentifier())).expectCreated().extractBody();

        configurations.update(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), created.identifier(),
                factory.update(created.version() + 1)).expectConflict();
    }

    @Test
    @DisplayName("UPDATE - substitui publishers pelos configurados no workspace")
    void atualizaPublishersCopiandoDoWorkspace() {
        var created = configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(),
                factory.valid(scenario.environmentIdentifier())).expectCreated().extractBody();

        configurations.update(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), created.identifier(),
                        factory.updateCopyingWorkspacePublishers(created.version()))
                .expectOk().expect("version", created.version().intValue() + 1)
                .expectSize("publishers", 1)
                .expect("publishers[0].publisherCode", "KAFKA")
                .expect("publishers[0].parameters.topicName", "test-topic");
    }
}
