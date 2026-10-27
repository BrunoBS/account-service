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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

@DisplayName("Integração HTTP - Workspace Delete")
@WithMySql(excludeTables = {"flyway_schema_history"})
class WorkspaceDeleteIntegrationTest extends BaseIntegrationTest {
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
    @DisplayName("LIFECYCLE - só permite quarentenar workspace inativo")
    void soPermiteQuarentenarWorkspaceInativo() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Quarantine"))
                .expectCreated()
                .extractBody();

        workspaces.delete(created.identifier()).expectBadRequest().expect("code", "WORKSPACE-0003");
        workspaces.inactivate(created.identifier()).expectNoContent();
        workspaces.delete(created.identifier()).expectNoContent();
        workspaces.restore(created.identifier()).expectBadRequest().expect("code", "WORKSPACE-0002");
        workspaces.delete(created.identifier()).expectBadRequest().expect("code", "WORKSPACE-0003");
    }

    @Test
    @DisplayName("TAG - remove tags da busca depois da exclusão física")
    void removeTagsAposExclusaoFisica() {
        var created = workspaces.create(workspaceFactory.withManualTags(
                        "Workspace Delete Tags", "DEL", "DELETE", List.of("delete-tag")))
                .expectCreated().extractBody();

        workspaces.inactivate(created.identifier()).expectNoContent();
        workspaces.delete(created.identifier()).expectNoContent();
        workspaces.findAll().tagName("delete-tag").execute().expectOk()
                .expectNotContainsItem("content.identifier", created.identifier());
    }

    @Test
    @DisplayName("NOT FOUND - não exclui um workspace inexistente")
    void rejeitaExclusaoDeWorkspaceInexistente() {
        workspaces.delete("00000000-0000-0000-0000-000000000000")
                .expectNotFound().expect("code", "WORKSPACE-0001");
    }

    @Test
    @DisplayName("AUTH - oculta a exclusão fora do grupo autorizado")
    void ocultaExclusaoForaDoGrupoAutorizado() {
        var created = workspaces.create(workspaceFactory.adminWithAuthorizerGroup(
                        "Workspace Protected Delete", "TEAM_B"))
                .expectCreated().extractBody();
        authorization.reset();
        authorization.allow(session -> session.userName("owner-it").groups("PM5_OWNER"));
        workspaces.inactivate(created.identifier()).expectNoContent();

        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_ADM_TEAM_A", "ADM", "ADM", "TEAM_A"));
        workspaces.delete(created.identifier()).expectNotFound().expect("code", "WORKSPACE-0001");
        authorization.verifyCalledWithPolicy("ADM");
    }

    @Test
    @DisplayName("AUDIT - registra a exclusão lógica e o snapshot em quarentena")
    void registraAuditoriaDaExclusao() {
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER").userName("auditor")
                .accountId("audit-account").applicationId("audit-application")
                .environmentId("audit-environment").traceId("audit-trace"));
        var created = workspaces.create(workspaceFactory.withManualTags(
                        "Workspace Audit Delete", "WSP", "AUDIT_TEAM", List.of("audit-manual")))
                .expectCreated().extractBody();
        workspaces.inactivate(created.identifier()).expectNoContent();
        clearInvocations(auditAppender);

        workspaces.delete(created.identifier()).expectNoContent();

        ArgumentCaptor<JsonNode> payload = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<JsonNode> metadata = ArgumentCaptor.forClass(JsonNode.class);
        verify(auditAppender).append(payload.capture(), metadata.capture());
        assertEquals("WORKSPACE_DELETED", metadata.getValue().path("eventType").asText());
        assertEquals(created.identifier(), metadata.getValue().path("resourceIdentifier").asText());
        assertEquals("QUARANTINED", payload.getValue().path("lifecycle").asText());
        assertTrue(payload.getValue().path("tags").toString().contains("audit-manual"));
    }
}
