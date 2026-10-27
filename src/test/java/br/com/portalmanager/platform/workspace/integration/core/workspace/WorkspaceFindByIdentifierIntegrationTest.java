package br.com.portalmanager.platform.workspace.integration.core.workspace;

import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceFactory;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceScenario;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingPhaseFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("Integração HTTP - Workspace FindByIdentifier")
@WithMySql(excludeTables = {"flyway_schema_history"})
class WorkspaceFindByIdentifierIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private WorkspaceTypeClient workspaceTypes;
    @Autowired private OnboardingTypeClient onboardingTypes;
    @Autowired private WorkspaceClient workspaces;
    @Autowired private SchemaScenario schemaScenario;

    private final WorkspaceFactory workspaceFactory = new WorkspaceFactory();
    private final WorkspaceTypeFactory workspaceTypeFactory = new WorkspaceTypeFactory();
    private final OnboardingPhaseFactory onboardingPhaseFactory = new OnboardingPhaseFactory();

    @BeforeEach
    void prepareWorkspaceScenario() {
        new WorkspaceScenario(authorization, workspaceTypes, workspaceTypeFactory,
                onboardingTypes, onboardingPhaseFactory, schemaScenario).setup();
    }

    @Test
    @DisplayName("NOT FOUND - não encontra um workspace inexistente")
    void rejeitaBuscaDeWorkspaceInexistente() {
        String identifier = "00000000-0000-0000-0000-000000000000";
        workspaces.findById(identifier).expectNotFound().expect("code", "WORKSPACE-0001");
    }

    @Test
    @DisplayName("AUTH - exige autenticação e os dois headers de protocolo")
    void exigeAutenticacaoEHeaders() {
        workspaces.findWithoutAuthenticationHeaders().expectUnauthorized()
                .expect("code", "AUTH-401-001");
        workspaces.findWithoutCorrelationId().expectUnauthorized()
                .expect("code", "AUTH-401-001");
        workspaces.findWithoutBearerToken().expectUnauthorized()
                .expect("code", "AUTH-401-002");
    }

    @Test
    @DisplayName("AUTH - limita a consulta individual pela visibilidade do grupo")
    void limitaConsultaIndividualPorVisibilidadeDoGrupo() {
        var teamA = workspaces.create(workspaceFactory.adminWithAuthorizerGroup("Workspace Team A", "TEAM_A"))
                .expectCreated().extractBody();
        var teamB = workspaces.create(workspaceFactory.adminWithAuthorizerGroup("Workspace Team B", "TEAM_B"))
                .expectCreated().extractBody();

        allowTeamADeveloper();
        workspaces.findById(teamA.identifier()).expectOk().expect("identifier", teamA.identifier());
        workspaces.findById(teamB.identifier()).expectNotFound().expect("code", "WORKSPACE-0001");

        allowOwner();
        workspaces.findById(teamB.identifier()).expectOk().expect("identifier", teamB.identifier());
    }

    @Test
    @DisplayName("AUTH - aplica a política DEV na consulta individual")
    void aplicaPoliticaDevNaConsultaIndividual() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Policy"))
                .expectCreated().extractBody();
        allowOwner();
        workspaces.findById(created.identifier()).expectOk();
        authorization.verifyCalledWithPolicy("DEV");
    }

    private void allowOwner() {
        authorization.reset();
        authorization.allow(session -> session.userName("owner-it").groups("PM5_OWNER"));
    }

    private void allowTeamADeveloper() {
        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_DEV_TEAM_A", "DEV", "DEV", "TEAM_A"));
    }
}
