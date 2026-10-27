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

@DisplayName("Integração HTTP - Application Environment Configuration Create")
@WithMySql(excludeTables = {"flyway_schema_history"})
class ApplicationEnvironmentConfigurationCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private ApplicationEnvironmentConfigurationScenario scenario;
    @Autowired private ApplicationEnvironmentConfigurationClient configurations;
    private final ApplicationEnvironmentConfigurationFactory factory = new ApplicationEnvironmentConfigurationFactory();

    @BeforeEach void prepareScenario() { scenario.setup(); }

    @Test @DisplayName("CREATE - associa settings e publisher a uma aplicação e ambiente")
    void criaConfiguracaoDaAplicacao() {
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), factory.valid(scenario.environmentIdentifier()))
                .expectCreated().expect("lifecycle", "ACTIVE").expect("environmentIdentifier", scenario.environmentIdentifier())
                .expect("settings.region", "test").expect("publishers[0].publisherCode", "KAFKA");
    }

    @Test @DisplayName("VALIDATION - rejeita ambiente ausente, settings inválidos e publisher não configurado")
    void rejeitaEntradasInvalidas() {
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), factory.withoutEnvironment()).expectBadRequest();
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), factory.withInvalidSettings(scenario.environmentIdentifier())).expectBadRequest();
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), factory.withMissingPublisher(scenario.environmentIdentifier()))
                .expectBadRequest();
    }

    @Test @DisplayName("VALIDATION - exige publishers explícitos quando não copia os publishers do workspace")
    void exigePublishersQuandoNaoCopiaDoWorkspace() {
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), factory.withoutPublishers(scenario.environmentIdentifier()))
                .expectBadRequest();
    }

    @Test @DisplayName("CREATE - copia os publishers configurados no workspace")
    void copiaPublishersDoWorkspace() {
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(),
                        factory.copyWorkspacePublishers(scenario.environmentIdentifier()))
                .expectCreated().expectSize("publishers", 1)
                .expect("publishers[0].publisherCode", "KAFKA")
                .expect("publishers[0].parameters.topicName", "test-topic");
    }

    @Test @DisplayName("CREATE - exige configuração do workspace para configurar o ambiente da aplicação")
    void exigeConfiguracaoDoWorkspace() {
        String environmentWithoutWorkspaceConfiguration = scenario.createEnvironmentWithoutWorkspaceConfiguration();
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(),
                        factory.valid(environmentWithoutWorkspaceConfiguration))
                .expectNotFound();
    }

    @Test @DisplayName("CREATE - permite publisher com escopo de aplicação")
    void criaConfiguracaoComPublisherDeEscopoAplicacao() {
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(),
                        factory.withApplicationPublisher(scenario.environmentIdentifier()))
                .expectCreated().expect("publishers[0].publisherCode", "APP_CONFIG")
                .expect("publishers[0].parameters.contaAWS", "123456789012");
    }
}
