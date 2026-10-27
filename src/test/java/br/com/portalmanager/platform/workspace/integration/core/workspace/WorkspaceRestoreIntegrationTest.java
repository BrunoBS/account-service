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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

@DisplayName("Integração HTTP - Workspace Restore")
@WithMySql(excludeTables = {"flyway_schema_history"})
class WorkspaceRestoreIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("TAG - restaura workspace preservando tags manuais e sistêmicas")
    void restauraWorkspacePreservandoTags() {
        var created = workspaces.create(workspaceFactory.withManualTags(
                        "Workspace Restore Tags", "RST", "RESTORE", List.of("manual-restore")))
                .expectCreated().extractBody();
        var originalTags = created.tags();

        workspaces.inactivate(created.identifier()).expectNoContent();
        var restored = workspaces.restore(created.identifier()).expectOk().extractBody();

        org.junit.jupiter.api.Assertions.assertEquals(originalTags, restored.tags());
        workspaces.findAll().tagName("Workspace Restore Tags").execute().expectOk()
                .expectContainsItem("content.identifier", created.identifier());
        workspaces.findAll().tagName("RESTORE").execute().expectOk()
                .expectContainsItem("content.identifier", created.identifier());
        workspaces.findAll().tagName("manual-restore").execute().expectOk()
                .expectContainsItem("content.identifier", created.identifier());
    }

    @Test
    @DisplayName("LIFECYCLE - restaura somente um workspace inativo")
    void restauraSomenteWorkspaceInativo() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Restore Lifecycle"))
                .expectCreated().extractBody();
        workspaces.restore(created.identifier()).expectBadRequest().expect("code", "WORKSPACE-0002");
        workspaces.inactivate(created.identifier()).expectNoContent();

        workspaces.restore(created.identifier()).expectOk()
                .expect("identifier", created.identifier())
                .expect("lifecycle", "ACTIVE");
        workspaces.restore(created.identifier()).expectBadRequest().expect("code", "WORKSPACE-0002");
    }

    @Test
    @DisplayName("NOT FOUND - não restaura um workspace inexistente")
    void rejeitaRestauracaoDeWorkspaceInexistente() {
        workspaces.restore("00000000-0000-0000-0000-000000000000")
                .expectNotFound().expect("code", "WORKSPACE-0001");
    }

    @Test
    @DisplayName("AUTH - oculta a restauração fora do grupo autorizado")
    void ocultaRestauracaoForaDoGrupoAutorizado() {
        var created = workspaces.create(workspaceFactory.adminWithAuthorizerGroup(
                        "Workspace Protected Restore", "TEAM_B"))
                .expectCreated().extractBody();
        authorization.reset();
        authorization.allow(session -> session.userName("owner-it").groups("PM5_OWNER"));
        workspaces.inactivate(created.identifier()).expectNoContent();

        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_ADM_TEAM_A", "ADM", "ADM", "TEAM_A"));
        workspaces.restore(created.identifier()).expectNotFound().expect("code", "WORKSPACE-0001");
        authorization.verifyCalledWithPolicy("ADM");
    }

    @Test
    @DisplayName("AUDIT - registra a restauração e o snapshot do workspace")
    void registraAuditoriaDaRestauracao() {
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER").userName("auditor")
                .accountId("audit-account").applicationId("audit-application")
                .environmentId("audit-environment").traceId("audit-trace"));
        var created = workspaces.create(workspaceFactory.admin("Workspace Audit Restore"))
                .expectCreated().extractBody();
        workspaces.inactivate(created.identifier()).expectNoContent();
        clearInvocations(auditAppender);

        workspaces.restore(created.identifier()).expectOk();

        ArgumentCaptor<JsonNode> payload = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<JsonNode> metadata = ArgumentCaptor.forClass(JsonNode.class);
        verify(auditAppender).append(payload.capture(), metadata.capture());
        assertEquals("WORKSPACE_RESTORED", metadata.getValue().path("eventType").asText());
        assertEquals(created.identifier(), metadata.getValue().path("resourceIdentifier").asText());
        assertEquals("ACTIVE", payload.getValue().path("lifecycle").asText());
    }
}
