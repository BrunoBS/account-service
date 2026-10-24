package br.com.portalmanager.platform.workspace.entrypoint.workspace.integration;

import br.com.portalmanager.platform.workspace.entrypoint.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.support.WorkspaceFactory;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.support.WorkspaceScenario;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.onboarding.OnboardingPhaseFactory;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.onboarding.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.workspace.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.workspace.WorkspaceTypeFactory;
import br.com.portalmanager.platform.workspace.entrypoint.schema.SchemaScenario;
import br.com.portalmanager.platform.workspace.support.BaseIntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

@DisplayName("Integração HTTP - Workspace Inactivate")
@WithMySql(excludeTables = {
        "flyway_schema_history",
        "type_life_cycle",
        "type_schema_scopes",
        "type_schema_version_status"
})
class WorkspaceInactivateIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("LIFECYCLE - inativa o workspace")
    void inativaWorkspace() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Lifecycle"))
                .expectCreated().extractBody();

        workspaces.inactivate(created.identifier()).expectNoContent();
        workspaces.findById(created.identifier()).expectNotFound()
                .expect("code", "WORKSPACE-0001");
    }

    @Test
    @DisplayName("NOT FOUND - não inativa um workspace inexistente")
    void rejeitaInativacaoDeWorkspaceInexistente() {
        workspaces.inactivate("00000000-0000-0000-0000-000000000000")
                .expectNotFound().expect("code", "WORKSPACE-0001");
    }

    @Test
    @DisplayName("AUTH - oculta a inativação fora do grupo autorizado")
    void ocultaInativacaoForaDoGrupoAutorizado() {
        var created = workspaces.create(workspaceFactory.adminWithAuthorizerGroup(
                        "Workspace Protected Inactivate", "TEAM_B"))
                .expectCreated().extractBody();
        allowTeamAAdministrator();
        workspaces.inactivate(created.identifier()).expectNotFound().expect("code", "WORKSPACE-0001");

        allowOwner();
        workspaces.inactivate(created.identifier()).expectNoContent();
        authorization.verifyCalledWithPolicy("ADM");
    }

    @Test
    @DisplayName("AUDIT - registra a inativação e o snapshot do workspace")
    void registraAuditoriaDaInativacao() {
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER").userName("auditor")
                .accountId("audit-account").applicationId("audit-application")
                .environmentId("audit-environment").traceId("audit-trace"));
        var created = workspaces.create(workspaceFactory.withManualTags(
                        "Workspace Audit Inactivate", "WSP", "AUDIT_TEAM", List.of("audit-manual")))
                .expectCreated().extractBody();
        clearInvocations(auditAppender);

        workspaces.inactivate(created.identifier()).expectNoContent();

        ArgumentCaptor<JsonNode> payload = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<JsonNode> metadata = ArgumentCaptor.forClass(JsonNode.class);
        verify(auditAppender).append(payload.capture(), metadata.capture());
        assertEquals("WORKSPACE_DEACTIVATED", metadata.getValue().path("eventType").asText());
        assertEquals(created.identifier(), metadata.getValue().path("resourceIdentifier").asText());
        assertEquals("INACTIVE", payload.getValue().path("lifecycle").asText());
        assertTrue(payload.getValue().path("tags").toString().contains("audit-manual"));
    }

    private void allowOwner() {
        authorization.reset();
        authorization.allow(session -> session.userName("owner-it").groups("PM5_OWNER"));
    }

    private void allowTeamAAdministrator() {
        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_ADM_TEAM_A", "ADM", "ADM", "TEAM_A"));
    }
}
