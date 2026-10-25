package br.com.portalmanager.platform.workspace.integration.core.application;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationClient;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationFactory;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationScenario;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaScenario;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Application FindByIdentifier")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class ApplicationFindByIdentifierIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private WorkspaceClient workspaces;
    @Autowired private WorkspaceTypeClient workspaceTypes;
    @Autowired private OnboardingTypeClient onboardingTypes;
    @Autowired private ApplicationScopeTypeClient applicationScopes;
    @Autowired private SchemaScenario schemas;
    @Autowired private ApplicationClient applications;
    private final ApplicationFactory factory = new ApplicationFactory();
    private ApplicationScenario scenario;

    @BeforeEach void prepareScenario() {
        scenario = new ApplicationScenario(authorization, workspaces, workspaceTypes, onboardingTypes, applicationScopes, schemas).setup();
    }

    @Test
    @DisplayName("QUERY - busca aplicação ativa pelo identificador e workspace")
    void buscaAplicacaoAtiva() {
        var created = applications.create(scenario.workspaceIdentifier(), factory.valid("Visible application"))
                .expectCreated()
                .extractBody();

        applications.findByIdentifier(scenario.workspaceIdentifier(), created.identifier())
                .expectOk()
                .expect("identifier", created.identifier())
                .expect("name", "Visible application");
    }

    @Test
    @DisplayName("NOT FOUND - não encontra aplicação inexistente ou pertencente a outro workspace")
    void naoEncontraAplicacaoForaDoWorkspace() {
        var created = applications.create(scenario.workspaceIdentifier(), factory.valid())
                .expectCreated()
                .extractBody();
        String otherWorkspace = scenario.createAdminWorkspace("Other workspace");

        applications.findByIdentifier(scenario.workspaceIdentifier(), "00000000-0000-0000-0000-000000000000")
                .expectNotFound()
                .expect("code", "APPLICATION-0001");

        applications.findByIdentifier(otherWorkspace, created.identifier())
                .expectNotFound()
                .expect("code", "APPLICATION-0001");
    }

    @Test
    @DisplayName("QUERY - aplicação inativa não fica disponível na consulta individual")
    void naoEncontraAplicacaoInativa() {
        var created = applications.create(scenario.workspaceIdentifier(), factory.valid())
                .expectCreated()
                .extractBody();

        applications.inactivate(scenario.workspaceIdentifier(), created.identifier()).expectNoContent();

        applications.findByIdentifier(scenario.workspaceIdentifier(), created.identifier())
                .expectNotFound()
                .expect("code", "APPLICATION-0001");
    }

    @Test
    @DisplayName("AUTH - limita a consulta individual à visibilidade do workspace e da aplicação")
    void limitaConsultaAosGruposVisiveis() {
        String workspace = scenario.createManagerWorkspace("Visible application workspace", "PARENT_A");
        var teamA = applications.create(workspace, factory.withAuthorizerGroup("TEAM_A"))
                .expectCreated()
                .extractBody();

        var teamB = applications.create(workspace, factory.withAuthorizerGroup("TEAM_B"))
                .expectCreated()
                .extractBody();

        authorization.reset();

        authorization.allow(session -> session.groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_DEV_PARENT_A", "DEV", "DEV", "PARENT_A")
                .addAuthorizerGroup("GRP_APPLICATION_DEV_TEAM_A", "DEV", "DEV", "A-TEAM_A"));
        applications.findByIdentifier(workspace, teamA.identifier())
                .expectOk()
                .expect("identifier", teamA.identifier());

        applications.findByIdentifier(workspace, teamB.identifier())
                .expectNotFound()
                .expect("code", "APPLICATION-0001");

        authorization.verifyCalledWithPolicy("DEV");

    }
}
