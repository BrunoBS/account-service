package br.com.portalmanager.platform.workspace.integration.core.applicationconfiguration;

import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import br.com.portalmanager.platform.workspace.integration.core.applicationconfiguration.support.*;
import br.com.portalmanager.platform.workspace.integration.core.workspaceconfiguration.support.WorkspaceEnvironmentConfigurationClient;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationClient;
import br.com.portalmanager.platform.workspace.integration.core.environment.workspace.support.WorkspaceEnvironmentClient;
import br.com.portalmanager.platform.workspace.entrypoint.applicationconfiguration.response.ApplicationEnvironmentConfigurationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Integração HTTP - Application Configuration prerequisites and rollback")
class ApplicationEnvironmentConfigurationLifecycleIntegrationTest extends BaseIntegrationTest {
    @Autowired private ApplicationEnvironmentConfigurationScenario scenario;
    @Autowired private ApplicationEnvironmentConfigurationClient configurations;
    @Autowired private WorkspaceEnvironmentConfigurationClient workspaceConfigurations;
    @Autowired private WorkspaceClient workspaces;
    @Autowired private br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock authorization;
    @Autowired private ApplicationClient applications;
    @Autowired private WorkspaceEnvironmentClient environments;
    private final ApplicationEnvironmentConfigurationFactory factory = new ApplicationEnvironmentConfigurationFactory();

    @BeforeEach void setup() { scenario.setup(); }

    @Test void bloqueiaCriacaoSemOrigemComOuSemCopia() {
        String environment = scenario.createEnvironmentWithoutWorkspaceConfiguration();
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), factory.valid(environment)).expectNotFound();
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), factory.copyWorkspacePublishers(environment)).expectNotFound();
        configurations.findAll(scenario.workspaceIdentifier(), scenario.applicationIdentifier()).execute().expectOk().expectSize("content", 0);
    }

    @Test void bloqueiaCriacaoComOrigemInativa() { assertCreateBlocked(false); }
    @Test void bloqueiaCriacaoComOrigemEmQuarentena() { assertCreateBlocked(true); }
    @Test void preservaConfiguracaoEAssociacoesQuandoOrigemInativa() { assertUpdateBlocked(false); }
    @Test void preservaConfiguracaoEAssociacoesQuandoOrigemEmQuarentena() { assertUpdateBlocked(true); }

    private void deactivateSource(boolean quarantine) {
        workspaceConfigurations.inactivate(scenario.workspaceIdentifier(), scenario.workspaceConfigurationIdentifier()).expectNoContent();
        if (quarantine) workspaceConfigurations.delete(scenario.workspaceIdentifier(), scenario.workspaceConfigurationIdentifier()).expectNoContent();
    }

    private void assertCreateBlocked(boolean quarantine) {
        deactivateSource(quarantine);
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), factory.valid(scenario.environmentIdentifier())).expectNotFound();
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), factory.copyWorkspacePublishers(scenario.environmentIdentifier())).expectNotFound();
        configurations.findAll(scenario.workspaceIdentifier(), scenario.applicationIdentifier()).execute().expectOk().expectSize("content", 0);
    }

    private void assertUpdateBlocked(boolean quarantine) {
        ApplicationEnvironmentConfigurationResponse before = create();
        deactivateSource(quarantine);
        configurations.update(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), before.identifier(), factory.update(before.version())).expectNotFound();
        configurations.update(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), before.identifier(), factory.updateCopyingWorkspacePublishers(before.version())).expectNotFound();
        assertUnchanged(before);
    }

    @Test void bloqueiaWorkspaceInativoERestauraAcessoSemReativarFilhos() {
        ApplicationEnvironmentConfigurationResponse before = create();
        workspaces.inactivate(scenario.workspaceIdentifier()).expectNoContent();
        applications.findByIdentifier(scenario.workspaceIdentifier(), scenario.applicationIdentifier()).expectNotFound();
        environments.find(scenario.workspaceIdentifier(), scenario.environmentIdentifier()).expectNotFound();
        assertWritesBlocked(before);
        workspaces.restore(scenario.workspaceIdentifier()).expectOk();
        applications.findByIdentifier(scenario.workspaceIdentifier(), scenario.applicationIdentifier()).expectOk().expect("lifecycle", "ACTIVE");
        environments.find(scenario.workspaceIdentifier(), scenario.environmentIdentifier()).expectOk().expect("lifecycle", "ACTIVE");
        assertUnchanged(before);
        applications.inactivate(scenario.workspaceIdentifier(), scenario.applicationIdentifier()).expectNoContent();
        workspaces.inactivate(scenario.workspaceIdentifier()).expectNoContent();
        workspaces.restore(scenario.workspaceIdentifier()).expectOk();
        applications.findByIdentifier(scenario.workspaceIdentifier(), scenario.applicationIdentifier()).expectNotFound();
    }

    @Test void bloqueiaApplicationInativa() {
        ApplicationEnvironmentConfigurationResponse before = create();
        applications.inactivate(scenario.workspaceIdentifier(), scenario.applicationIdentifier()).expectNoContent();
        assertWritesBlocked(before);
        applications.restore(scenario.workspaceIdentifier(), scenario.applicationIdentifier()).expectOk();
        assertUnchanged(before);
    }

    @Test void bloqueiaEnvironmentInativo() {
        ApplicationEnvironmentConfigurationResponse before = create();
        environments.inactivate(scenario.workspaceIdentifier(), scenario.environmentIdentifier()).expectNoContent();
        assertWritesBlocked(before);
        environments.restore(scenario.workspaceIdentifier(), scenario.environmentIdentifier()).expectOk();
        assertUnchanged(before);
    }

    @Test void authorizationOutageDoesNotGrantAccessOrChangeData() {
        ApplicationEnvironmentConfigurationResponse before = create();
        authorization.reset();
        authorization.internalError();
        try {
            configurations.update(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), before.identifier(), factory.update(before.version())).response().statusCode(503);
        } finally {
            authorization.reset();
            authorization.allow(session -> session.userName("owner-it").groups("PM5_OWNER"));
        }
        assertUnchanged(before);
    }

    @Test void rejectsConfigurationFromAnotherApplicationOrWorkspace() {
        ApplicationEnvironmentConfigurationResponse before = create();
        String otherApplication = scenario.createAnotherApplication();
        configurations.find(scenario.workspaceIdentifier(), otherApplication, before.identifier()).expectNotFound();
        configurations.update(scenario.workspaceIdentifier(), otherApplication, before.identifier(), factory.update(before.version())).expectNotFound();
        String otherWorkspace = workspaces.create(new br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceFactory().manager("Other isolated workspace")).expectCreated().extractBody().identifier();
        configurations.find(otherWorkspace, scenario.applicationIdentifier(), before.identifier()).expectNotFound();
        configurations.create(otherWorkspace, scenario.applicationIdentifier(), factory.valid(scenario.environmentIdentifier())).expectNotFound();
        assertUnchanged(before);
    }

    @Test void inactiveParentEnvironmentBlocksChildUntilRestored() {
        br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentFactory environmentFactory = new br.com.portalmanager.platform.workspace.integration.core.environment.support.EnvironmentFactory();
        String child = environments.create(scenario.workspaceIdentifier(), environmentFactory.shard("Child environment", scenario.environmentIdentifier())).expectCreated().extractBody().identifier();
        environments.find(scenario.workspaceIdentifier(), child).expectOk();
        environments.inactivate(scenario.workspaceIdentifier(), scenario.environmentIdentifier()).expectNoContent();
        environments.find(scenario.workspaceIdentifier(), child).expectNotFound();
        environments.create(scenario.workspaceIdentifier(), environmentFactory.shard("Blocked child", scenario.environmentIdentifier())).expectNotFound();
        environments.restore(scenario.workspaceIdentifier(), scenario.environmentIdentifier()).expectOk();
        environments.find(scenario.workspaceIdentifier(), child).expectOk().expect("lifecycle", "ACTIVE");
    }

    private void assertWritesBlocked(ApplicationEnvironmentConfigurationResponse before) {
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), factory.valid(scenario.environmentIdentifier())).expectNotFound();
        configurations.create(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), factory.copyWorkspacePublishers(scenario.environmentIdentifier())).expectNotFound();
        configurations.update(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), before.identifier(), factory.update(before.version())).expectNotFound();
        configurations.update(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), before.identifier(), factory.updateCopyingWorkspacePublishers(before.version())).expectNotFound();
    }

    private ApplicationEnvironmentConfigurationResponse create() {
        ApplicationEnvironmentConfigurationResponse created = configurations.create(
                scenario.workspaceIdentifier(),
                scenario.applicationIdentifier(),
                factory.valid(scenario.environmentIdentifier()))
            .expectCreated()
            .extractBody();

        // DATETIME(6) stores microseconds; take the baseline from persisted state
        // before comparing it after rejected operations.
        return configurations.find(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), created.identifier())
            .expectOk()
            .extractBody();
    }

    private void assertUnchanged(ApplicationEnvironmentConfigurationResponse before) {
        assertThat(configurations.find(scenario.workspaceIdentifier(), scenario.applicationIdentifier(), before.identifier()).expectOk().extractBody()).isEqualTo(before);
    }
}
