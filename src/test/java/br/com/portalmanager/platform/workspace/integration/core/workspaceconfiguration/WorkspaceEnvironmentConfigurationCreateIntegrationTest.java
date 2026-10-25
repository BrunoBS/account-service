package br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.integration.feature.onboarding.support.OnboardingClient;
import br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationClient;
import br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationFactory;
import br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Workspace Environment Configuration Create")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class WorkspaceEnvironmentConfigurationCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private WorkspaceEnvironmentConfigurationScenario scenario;
    @Autowired private WorkspaceEnvironmentConfigurationClient configurations;
    @Autowired private OnboardingClient onboarding;
    private final WorkspaceEnvironmentConfigurationFactory factory = new WorkspaceEnvironmentConfigurationFactory();

    @BeforeEach
    void prepareScenario() {
        scenario.setup();
    }

    @Test
    @DisplayName("CREATE - associa settings e publisher ao workspace e ambiente")
    void criaConfiguracaoDoWorkspace() {
        configurations.create(scenario.workspaceIdentifier(), factory.valid(scenario.environmentIdentifier()))
                .expectCreated()
                .expect("lifecycle", "ACTIVE")
                .expect("environmentIdentifier", scenario.environmentIdentifier())
                .expect("settings.region", "test")
                .expect("publishers[0].publisherCode", "KAFKA")
                .expect("publishers[0].parameters.topicName", "test-topic");
    }

    @Test
    @DisplayName("CREATE - permite settings nulo")
    void permiteSettingsNulo() {
        configurations.create(scenario.workspaceIdentifier(), factory.withNullSettings(scenario.environmentIdentifier()))
                .expectCreated().expect("settings", null);
    }

    @Test
    @DisplayName("CREATE - valida os parâmetros específicos do WebSocket")
    void criaConfiguracaoComWebSocket() {
        configurations.create(scenario.workspaceIdentifier(), factory.validWithWebSocket(scenario.environmentIdentifier()))
                .expectCreated()
                .expect("publishers[0].publisherCode", "WEB_SOCKET")
                .expect("publishers[0].parameters.clientId", "test-client");
    }

    @Test
    @DisplayName("CREATE - mantém a ordem definida para múltiplos publishers")
    void criaConfiguracaoComMultiplosPublishersOrdenados() {
        configurations.create(scenario.workspaceIdentifier(), factory.withMultiplePublishers(scenario.environmentIdentifier()))
                .expectCreated().expectSize("publishers", 2)
                .expect("publishers[0].publisherCode", "KAFKA")
                .expect("publishers[0].sortOrder", 1)
                .expect("publishers[1].publisherCode", "WEB_SOCKET")
                .expect("publishers[1].sortOrder", 2);
    }

    @Test
    @DisplayName("VALIDATION - rejeita request, settings e parâmetros do publisher inválidos")
    void rejeitaEntradasInvalidas() {
        configurations.create(scenario.workspaceIdentifier(), factory.withoutEnvironment()).expectBadRequest();
        configurations.create(scenario.workspaceIdentifier(), factory.withInvalidSettings(scenario.environmentIdentifier()))
                .expectBadRequest();
        configurations.create(scenario.workspaceIdentifier(), factory.withoutPublishers(scenario.environmentIdentifier()))
                .expectBadRequest();
        configurations.create(scenario.workspaceIdentifier(), factory.withMissingPublisherParameters(scenario.environmentIdentifier()))
                .expectBadRequest();
        configurations.create(scenario.workspaceIdentifier(), factory.withDuplicatePublisher(scenario.environmentIdentifier()))
                .expectBadRequest();
    }

    @Test
    @DisplayName("ONBOARDING - conclui a fase de configuração do primeiro ambiente")
    void registraPrimeiroAmbienteNoOnboarding() {
        configurations.create(scenario.workspaceIdentifier(), factory.valid(scenario.environmentIdentifier())).expectCreated();

        onboarding.findProgress(scenario.workspaceIdentifier()).expectOk()
                .expect("phases[1].code", "WORKSPACE_FIRST_ENVIRONMENT")
                .expect("phases[1].status", "COMPLETED");
    }
}
