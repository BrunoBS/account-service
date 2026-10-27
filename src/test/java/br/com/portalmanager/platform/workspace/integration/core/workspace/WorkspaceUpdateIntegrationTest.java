package br.com.portalmanager.platform.workspace.integration.core.workspace;


import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceUpdateFactory.InvalidUpdate;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceFactory;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceProtocolPayloads;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceResponse;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceScenario;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceUpdateFactory;


import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationClient;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationClientResponse;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationFactory;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationScenario;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingPhaseFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@DisplayName("Integração HTTP - Workspace Update")
@WithMySql(excludeTables = {"flyway_schema_history"})
class WorkspaceUpdateIntegrationTest extends BaseIntegrationTest {
    @Autowired protected AuthorizationMock authorization;
    @Autowired protected WorkspaceTypeClient workspaceTypes;
    @Autowired protected OnboardingTypeClient onboardingTypes;
    @Autowired protected WorkspaceClient workspaces;
    @Autowired protected SchemaScenario schemaScenario;

    protected final WorkspaceFactory workspaceFactory = new WorkspaceFactory();
    protected final WorkspaceUpdateFactory workspaceUpdateFactory = new WorkspaceUpdateFactory();
    protected final WorkspaceTypeFactory workspaceTypeFactory = new WorkspaceTypeFactory();
    protected final OnboardingPhaseFactory onboardingPhaseFactory = new OnboardingPhaseFactory();

    @BeforeEach
    void prepareWorkspaceScenario() {
        new WorkspaceScenario(authorization, workspaceTypes, workspaceTypeFactory,
                onboardingTypes, onboardingPhaseFactory, schemaScenario).setup();
    }

    @Autowired private ApplicationClient applications;
    @Autowired private ApplicationScopeTypeClient applicationScopes;
    private final ApplicationFactory applicationFactory = new ApplicationFactory();

    @Test
    @DisplayName("UPDATE - atualiza workspace com a versão atual")
    void atualizaWorkspaceComVersaoAtual() {
        var created = workspaces.create(workspaceFactory.admin("Workspace inicial"))
                .expectCreated()
                .extractBody();

        workspaces.update(created.identifier(), workspaceUpdateFactory.renamed(created, "Workspace atualizado"))
                .expectOk()
                .expect("identifier", created.identifier())
                .expect("version", created.version().intValue() + 1)
                .expect("name", "Workspace atualizado");
    }

    @Test
    @DisplayName("UPDATE - rejeita atualização com versão obsoleta")
    void rejeitaAtualizacaoComVersaoObsoleta() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Concorrente"))
                .expectCreated().extractBody();
        workspaces.update(created.identifier(), workspaceUpdateFactory.unchanged(created))
                .expectOk();

        workspaces.update(created.identifier(), workspaceUpdateFactory.unchanged(created))
                .expectConflict()
                .expect("code", "GLOBAL-0009");
    }

    @Test
    @DisplayName("CONCURRENCY - aceita apenas uma atualização concorrente com a mesma versão")
    void aceitaUmaAtualizacaoConcorrentePorVersao() throws Exception {
        var created = workspaces.create(workspaceFactory.admin("Workspace Concurrent Version"))
                .expectCreated().extractBody();
        CyclicBarrier startTogether = new CyclicBarrier(2);

        List<WorkspaceResponse> responses = concurrently(
                () -> {
                    startTogether.await(15, TimeUnit.SECONDS);
                    return workspaces.update(created.identifier(),
                            workspaceUpdateFactory.renamed(created, "Workspace Concurrent First"));
                },
                () -> {
                    startTogether.await(15, TimeUnit.SECONDS);
                    return workspaces.update(created.identifier(),
                            workspaceUpdateFactory.renamed(created, "Workspace Concurrent Second"));
                });

        assertEquals(List.of(200, 409), responses.stream()
                .map(WorkspaceResponse::statusCode).sorted().toList());
        responses.stream().filter(response -> response.statusCode() == 409).findFirst().orElseThrow()
                .expect("code", "GLOBAL-0009");
        var updated = workspaces.findById(created.identifier()).expectOk();
        updated.expect("version", created.version().intValue() + 1);
        assertTrue(List.of("Workspace Concurrent First", "Workspace Concurrent Second")
                .contains(updated.extractBody().name()));
    }

    @Test
    @DisplayName("NOT FOUND - não atualiza um workspace inexistente")
    void rejeitaAtualizacaoDeWorkspaceInexistente() {
        workspaces.update("00000000-0000-0000-0000-000000000000",
                        workspaceUpdateFactory.forNonexistentWorkspace())
                .expectNotFound().expect("code", "WORKSPACE-0001");
    }

    @Test
    @DisplayName("VALIDATION - exige versão no update")
    void exigeVersaoNoUpdate() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Shared Schema"))
                .expectCreated().extractBody();
        workspaces.update(created.identifier(), workspaceUpdateFactory.withVersion(created, null))
                .expectBadRequest().expectContainsItem("details.field", "version");

        workspaces.update(created.identifier(), workspaceUpdateFactory.withVersion(created, -1L))
                .expectBadRequest().expectContainsItem("details.field", "version");
        for (String invalidVersion : List.of("0.5", "\"0\"")) {
            workspaces.updateProtocolRequest(created.identifier(), WorkspaceProtocolPayloads.updateWithVersion(
                            created.name(), created.workspaceType(), invalidVersion))
                    .expectBadRequest().expectContainsItem("details.field", "version");
        }
        workspaces.update(created.identifier(), workspaceUpdateFactory.unchanged(created))
                .expectOk();
    }

    @Test
    @DisplayName("VALIDATION - rejeita payloads incompatíveis com o contrato HTTP do update")
    void rejeitaPayloadsIncompativeisNoUpdate() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Update Protocol"))
                .expectCreated().extractBody();

        for (String invalidJson : WorkspaceProtocolPayloads.incompatibleUpdateShapes(
                created.name(), created.workspaceType(), created.version())) {
            workspaces.updateProtocolRequest(created.identifier(), invalidJson)
                    .expectBadRequest().expect("code", "GLOBAL-0001");
        }
        workspaces.updateProtocolRequest(created.identifier(), "[").expectBadRequest();
        workspaces.findById(created.identifier()).expectOk()
                .expect("version", created.version().intValue());
    }

    @Test
    @DisplayName("VALIDATION - exige os campos obrigatórios no update")
    void exigeCamposObrigatoriosNoUpdate() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Required Update Fields"))
                .expectCreated().extractBody();

        for (WorkspaceUpdateFactory.InvalidUpdate invalid : workspaceUpdateFactory
                .missingRequiredFields(created.version())) {
            workspaces.update(created.identifier(), invalid.request()).expectBadRequest()
                    .expectContainsItem("details.field", invalid.field());
        }
    }

    @Test
    @DisplayName("VALIDATION - rejeita campos obrigatórios em branco no update")
    void rejeitaCamposObrigatoriosEmBrancoNoUpdate() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Blank Update Fields"))
                .expectCreated().extractBody();

        for (WorkspaceUpdateFactory.InvalidUpdate invalid : workspaceUpdateFactory
                .blankRequiredFields(created.version())) {
            workspaces.update(created.identifier(), invalid.request()).expectBadRequest()
                    .expectContainsItem("details.field", invalid.field());
        }
    }

    @Test
    @DisplayName("VALIDATION - rejeita e-mails inválidos no update")
    void rejeitaEmailsInvalidosNoUpdate() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Invalid Email Update"))
                .expectCreated().extractBody();

        for (WorkspaceUpdateFactory.InvalidUpdate invalid : workspaceUpdateFactory
                .invalidEmails(created.version())) {
            workspaces.update(created.identifier(), invalid.request()).expectBadRequest()
                    .expectContainsItem("details.field", invalid.field());
        }
    }

    @Test
    @DisplayName("VALIDATION - aceita comprimentos mínimos dos campos no update")
    void aceitaComprimentosMinimosNoUpdate() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Minimum Update Fields"))
                .expectCreated().extractBody();

        workspaces.update(created.identifier(), workspaceUpdateFactory.minimumLengths(created.version()))
                .expectOk()
                .expect("name", "Min")
                .expect("description", "1234567890")
                .expect("requester", "12345")
                .expect("acronym", "A")
                .expect("emailGroup", "a@b.com")
                .expect("approvers[0].functional", "F")
                .expect("approvers[0].email", "c@d.com");
    }

    @Test
    @DisplayName("VALIDATION - aceita configurações e tags nulas no update")
    void aceitaConfiguracoesETagsNulasNoUpdate() {
        var created = workspaces.create(workspaceFactory.withManualTags("Workspace Nullable Update",
                        "NULL", null, List.of("manual-tag")))
                .expectCreated().extractBody();

        workspaces.update(created.identifier(), workspaceUpdateFactory.withNullOptionalFields(created))
                .expectOk()
                .expect("settings", null)
                .expectNotContainsItem("tags", "manual-tag");
    }

    @Test
    @DisplayName("UPDATE - atualiza as configurações informadas")
    void atualizaConfiguracoes() throws Exception {
        var created = workspaces.create(workspaceFactory.admin("Workspace Update Settings"))
                .expectCreated().extractBody();
        JsonNode settings = JsonMapper.builder().build()
                .readTree("{\"region\":\"south\",\"flags\":{\"enabled\":true}}");

        workspaces.update(created.identifier(), workspaceUpdateFactory.withSettings(created, settings))
                .expectOk()
                .expect("settings.region", "south")
                .expect("settings.flags.enabled", true);
    }

    @Test
    @DisplayName("VALIDATION - rejeita lista de aprovadores vazia no update")
    void rejeitaListaDeAprovadoresVaziaNoUpdate() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Empty Approvers Update"))
                .expectCreated().extractBody();

        workspaces.update(created.identifier(), workspaceUpdateFactory.withoutApproverItems(created))
                .expectBadRequest().expectContainsItem("details.field", "approvers");
        workspaces.findById(created.identifier()).expectOk()
                .expect("version", created.version().intValue())
                .expectSize("approvers", 1);
    }

    @Test
    @DisplayName("NOT FOUND - não atualiza workspace inativo")
    void rejeitaAtualizacaoDeWorkspaceInativo() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Inactive Update"))
                .expectCreated().extractBody();
        workspaces.inactivate(created.identifier()).expectNoContent();

        workspaces.update(created.identifier(), workspaceUpdateFactory.renamed(created, "Inactive Renamed"))
                .expectNotFound().expect("code", "WORKSPACE-0001");
    }

    @Test
    @DisplayName("VALIDATION - rejeita tipo de workspace fora do catálogo no update")
    void rejeitaTipoDeWorkspaceForaDoCatalogoNoUpdate() {
        var created = workspaces.create(workspaceFactory.manager("Workspace Inactive Update Type"))
                .expectCreated().extractBody();
        workspaceTypes.delete("ADMIN").expectNoContent();

        workspaces.update(created.identifier(), workspaceUpdateFactory.changingType(created, "ADMIN"))
                .expectBadRequest().expectContainsItem("details.field", "workspaceType");
    }

    @Test
    @DisplayName("VALIDATION - rejeita valores de update maiores que os limites dos campos")
    void rejeitaValoresAcimaDosLimitesNoUpdate() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Update Limits"))
                .expectCreated().extractBody();
        for (WorkspaceUpdateFactory.InvalidUpdate invalid : workspaceUpdateFactory
                .aboveMaximumLengths(created)) {
            workspaces.update(created.identifier(), invalid.request()).expectBadRequest()
                    .expectContainsItem("details.field", invalid.field());
            workspaces.findById(created.identifier()).expectOk().expect("version", created.version().intValue());
        }
    }

    @Test
    @DisplayName("VALIDATION - limita aprovadores e tags no update e aceita os máximos")
    void limitaAprovadoresETags() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Collection Bounds"))
                .expectCreated().extractBody();
        for (WorkspaceUpdateFactory.InvalidUpdate invalid : workspaceUpdateFactory
                .overCollectionLimits(created)) {
            workspaces.update(created.identifier(), invalid.request()).expectBadRequest()
                    .expectContainsItem("details.field", invalid.field());
        }

        workspaces.update(created.identifier(), workspaceUpdateFactory.maximumCollections(created)).expectOk()
                .expectSize("approvers", 5)
                .expectSize("tags", 20);
    }

    @Test
    @DisplayName("VALIDATION - rejeita atualização para nome usado por outro grupo")
    void mantemUnicidadeGlobalDoNome() {
        var original = workspaces.create(workspaceFactory.adminWithAuthorizerGroup("Global Workspace Name", "TEAM_A"))
                .expectCreated().extractBody();
        var another = workspaces.create(workspaceFactory.adminWithAuthorizerGroup("Another Global Name", "TEAM_B"))
                .expectCreated().extractBody();
        workspaces.update(another.identifier(), workspaceUpdateFactory.renamed(another, original.name()))
                .expectBadRequest().expectContainsItem("details.field", "name");
    }

    @Test
    @DisplayName("VALIDATION - rejeita aprovadores duplicados no update")
    void rejeitaAprovadoresDuplicadosNoUpdate() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Duplicate Update"))
                .expectCreated().extractBody();
        workspaces.update(created.identifier(), workspaceUpdateFactory.duplicateApproverFunctionals(created))
                .expectBadRequest()
                .expect("code", "GLOBAL-0001")
                .expectContainsItem("details.field", "approvers[1].functional");
        workspaces.update(created.identifier(), workspaceUpdateFactory.duplicateApproverEmails(created))
                .expectBadRequest()
                .expect("code", "GLOBAL-0001")
                .expectContainsItem("details.field", "approvers[1].email");
    }

    @Test
    @DisplayName("UPDATE - substitui aprovadores e mantém os dados informados")
    void substituiAprovadoresNoUpdate() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Approvers"))
                .expectCreated().extractBody();
        var update = workspaceUpdateFactory.replacingApprovers(created, "Descrição atualizada com sucesso");

        workspaces.update(created.identifier(), update).expectOk()
                .expect("identifier", created.identifier())
                .expect("description", "Descrição atualizada com sucesso")
                .expectContainsItem("approvers.functional", "F2000")
                .expectContainsItem("approvers.functional", "F3000")
                .expectSize("approvers", 2);
    }

    @Test
    @DisplayName("UPDATE - preserva aprovadores existentes e permite trocar e-mails sem perder identidade")
    void preservaAprovadoresExistentesETrocaEmails() {
        var request = workspaceFactory.withWorkspaceForApproverReconciliation();
        var created = workspaces.create(request).expectCreated().extractBody();
        var swappedEmails = workspaceUpdateFactory.swappingApproverEmails(created);
        workspaces.update(created.identifier(), swappedEmails).expectOk();
        workspaces.findById(created.identifier()).expectOk()
                .expect("approvers[0].functional", "F1000")
                .expect("approvers[0].email", "second@example.com")
                .expect("approvers[1].functional", "F2000")
                .expect("approvers[1].email", "first@example.com");
    }

    @Test
    @DisplayName("UPDATE - mantém aprovador inalterado ao substituir outro")
    void mantemAprovadorInalteradoAoSubstituirOutro() {
        var created = workspaces.create(workspaceFactory.withWorkspaceForApproverReconciliation())
                .expectCreated().extractBody();
        var retained = created.approvers().getFirst();

        workspaces.update(created.identifier(),
                        workspaceUpdateFactory.retainingFirstAndReplacingSecond(created))
                .expectOk()
                .expectSize("approvers", 2)
                .expectContainsItem("approvers.functional", retained.functional())
                .expectContainsItem("approvers.email", retained.email())
                .expectContainsItem("approvers.functional", "F3000")
                .expectContainsItem("approvers.email", "replacement@example.com")
                .expectNotContainsItem("approvers.functional", "F2000");
    }

    @Test
    @DisplayName("TAG - recalcula tags do sistema e preserva as tags manuais no update")
    void recalculaTagsDoSistemaNoUpdate() {
        var created = workspaces.create(workspaceFactory.withManualTags("Workspace Original", "ORG",
                        "TEAM_A", List.of("manual-tag")))
                .expectCreated().extractBody();
        var update = workspaceUpdateFactory.withTagsAndMetadata(created, "Workspace Atualizado",
                "ATU", "TEAM_B", List.of("manual-tag"));

        workspaces.update(created.identifier(), update).expectOk()
                .expectContainsItem("tags", "manual-tag");
        workspaces.findAll().tagName("Workspace Original").execute().expectOk()
                .expectNotContainsItem("content.identifier", created.identifier());
        workspaces.findAll().tagName("ORG").execute().expectOk()
                .expectNotContainsItem("content.identifier", created.identifier());
        workspaces.findAll().tagName("Workspace Atualizado").execute().expectOk()
                .expectContainsItem("content.identifier", created.identifier());
        workspaces.findAll().tagName("ATU").execute().expectOk()
                .expectContainsItem("content.identifier", created.identifier());
        workspaces.findAll().tagName("manual-tag").execute().expectOk()
                .expectContainsItem("content.identifier", created.identifier());
    }

    @Test
    @DisplayName("UPDATE - normaliza os campos de texto enviados")
    void normalizaCamposDeTextoNoUpdate() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Before Normalization"))
                .expectCreated().extractBody();

        workspaces.update(created.identifier(), workspaceUpdateFactory.withNormalizedTextFields(created))
                .expectOk()
                .expect("workspaceType", "ADMIN")
                .expect("name", "Workspace Update Normalized")
                .expect("description", "Description normalized by update")
                .expect("requester", "updater")
                .expect("acronym", "UPD")
                .expect("authorizerGroup", "TEAM_A")
                .expect("emailGroup", "update@example.com")
                .expect("approvers[0].functional", "F3000")
                .expect("approvers[0].email", "approver@example.com");
    }

    @Test
    @DisplayName("UPDATE - impede trocar o tipo enquanto há aplicações em qualquer lifecycle")
    void impedeTrocaDeTipoComAplicacoesEmQualquerLifecycle() {
        new ApplicationScenario(schemaScenario, applicationScopes).setup();
        var workspace = workspaces.create(workspaceFactory.manager("Workspace With Application"))
                .expectCreated().extractBody();
        var application = applications.create(workspace.identifier(), applicationFactory.valid("Linked App"))
                .expectCreated();
        String applicationIdentifier = application.identifier();

        workspaces.update(workspace.identifier(), workspaceUpdateFactory.changingType(workspace, "ADMIN"))
                .expectBadRequest().expectContainsItem("details.field", "workspaceType");
        applications.inactivate(workspace.identifier(), applicationIdentifier).expectNoContent();
        workspaces.update(workspace.identifier(), workspaceUpdateFactory.changingType(workspace, "ADMIN"))
                .expectBadRequest().expectContainsItem("details.field", "workspaceType");
        applications.delete(workspace.identifier(), applicationIdentifier).expectNoContent();
        workspaces.update(workspace.identifier(), workspaceUpdateFactory.changingType(workspace, "ADMIN"))
                .expectBadRequest().expectContainsItem("details.field", "workspaceType");

        workspaces.update(workspace.identifier(), workspaceUpdateFactory.renamed(workspace, "Workspace With Application Edited"))
                .expectOk().expect("workspaceType", "MANAGER");
    }

    @Test
    @DisplayName("UPDATE - permite trocar o tipo quando não há aplicações")
    void permiteTrocaDeTipoSemAplicacoes() {
        var workspace = workspaces.create(workspaceFactory.manager("Workspace Without Application"))
                .expectCreated().extractBody();

        workspaces.update(workspace.identifier(), workspaceUpdateFactory.changingType(workspace, "ADMIN"))
                .expectOk().expect("workspaceType", "ADMIN");
    }

    @Test
    @DisplayName("CONCURRENCY - permite somente uma renomeação concorrente e preserva a outra workspace")
    void traduzColisaoConcorrenteDeRenomeacao() throws Exception {
        var first = workspaces.create(workspaceFactory.admin("Workspace Rename First"))
                .expectCreated().extractBody();
        var second = workspaces.create(workspaceFactory.admin("Workspace Rename Second"))
                .expectCreated().extractBody();
        String collisionName = "Concurrent Workspace Rename";
        CyclicBarrier nameChecks = new CyclicBarrier(2);
        doAnswer(invocation -> {
            nameChecks.await(15, TimeUnit.SECONDS);
            return false;
        }).when(workspaceRepository).existsByNameAndIdNot(matches("(?i)" + collisionName), anyLong());

        List<WorkspaceResponse> responses = concurrently(
                () -> workspaces.update(first.identifier(), workspaceUpdateFactory.renamed(first, collisionName)),
                () -> workspaces.update(second.identifier(), workspaceUpdateFactory.renamed(second, collisionName.toLowerCase(java.util.Locale.ROOT))));
        assertEquals(List.of(200, 409), responses.stream().map(WorkspaceResponse::statusCode).sorted().toList());

        WorkspaceResponse rejected = responses.stream().filter(response -> response.statusCode() == 409)
                .findFirst().orElseThrow();
        rejected.expect("code", "GLOBAL-0004").expectNotNull("message").expectSize("details", 0);
        var finalWorkspaces = workspaces.findAll().execute().expectOk();
        finalWorkspaces.expect("page.totalElements", 2)
                .expectContainsItem("content.identifier", first.identifier())
                .expectContainsItem("content.identifier", second.identifier());
        List<String> finalNames = finalWorkspaces.extractList("content.name", String.class);
        assertTrue(finalNames.contains(collisionName)
                || finalNames.contains(collisionName.toLowerCase(java.util.Locale.ROOT)));
    }

    @Test
    @DisplayName("CONCURRENCY - serializa criação de aplicação e troca de tipo do workspace")
    void serializaCriacaoDeAplicacaoETrocaDeTipo() throws Exception {
        new ApplicationScenario(schemaScenario, applicationScopes).setup();
        var workspace = workspaces.create(workspaceFactory.manager("Workspace Application Type Race"))
                .expectCreated().extractBody();
        CyclicBarrier operations = new CyclicBarrier(2);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var application = executor.submit(() -> {
                operations.await(15, TimeUnit.SECONDS);
                return applications.create(workspace.identifier(), applicationFactory.valid("Concurrent Application"));
            });
            var typeChange = executor.submit(() -> {
                operations.await(15, TimeUnit.SECONDS);
                return workspaces.update(workspace.identifier(),
                        workspaceUpdateFactory.changingType(workspace, "ADMIN"));
            });
            ApplicationClientResponse applicationResponse = application.get(30, TimeUnit.SECONDS);
            WorkspaceResponse updateResponse = typeChange.get(30, TimeUnit.SECONDS);

            int applicationStatus = applicationResponse.statusCode();
            int updateStatus = updateResponse.statusCode();
            assertTrue((applicationStatus == 201 && updateStatus == 400)
                    || (applicationStatus == 400 && updateStatus == 200));

            var finalWorkspace = workspaces.findById(workspace.identifier()).expectOk().extractBody();
            if (applicationStatus == 201) {
                assertEquals("MANAGER", finalWorkspace.workspaceType());
                updateResponse.expectBadRequest();
            } else {
                assertEquals("ADMIN", finalWorkspace.workspaceType());
                applicationResponse.expectBadRequest();
            }
        }
    }

    @Test
    @DisplayName("AUTH - oculta a atualização fora do grupo autorizado")
    void ocultaAtualizacaoForaDoGrupoAutorizado() {
        var teamB = workspaces.create(workspaceFactory.adminWithAuthorizerGroup("Workspace Protected", "TEAM_B"))
                .expectCreated().extractBody();
        allowTeamAAdministrator();

        workspaces.update(teamB.identifier(), workspaceUpdateFactory.unchanged(teamB))
                .expectNotFound().expect("code", "WORKSPACE-0001");

        allowOwner();
        var teamA = workspaces.create(workspaceFactory.adminWithAuthorizerGroup("Workspace Writable Team A", "TEAM_A"))
                .expectCreated().extractBody();
        allowTeamAAdministrator();
        workspaces.update(teamA.identifier(), workspaceUpdateFactory.renamed(teamA, "Workspace Writable Team A Updated"))
                .expectOk().expect("name", "Workspace Writable Team A Updated");
    }

    @Test
    @DisplayName("AUTH - exige permissão ADM para atualizar workspace")
    void exigePermissaoAdmParaAtualizar() {
        var created = workspaces.create(workspaceFactory.manager("Workspace ADM Permission"))
                .expectCreated().extractBody();
        authorization.reset();
        authorization.allow(session -> session.userName("regular-user").groups("USER"));

        workspaces.update(created.identifier(), workspaceUpdateFactory.renamed(created, "Unauthorized Rename"))
                .expectForbidden();
    }

    @Test
    @DisplayName("AUTH - aplica a política ADM na atualização")
    void aplicaPoliticaAdmNaAtualizacao() {
        var created = workspaces.create(workspaceFactory.admin("Workspace Update Policy"))
                .expectCreated().extractBody();
        allowOwner();

        workspaces.update(created.identifier(), workspaceUpdateFactory.renamed(created, "Workspace Update Policy Changed"))
                .expectOk();

        authorization.verifyCalledWithPolicy("ADM");
    }

    @Test
    @DisplayName("AUTH - exige OWNER para trocar tipo ou grupo autorizador")
    void exigeOwnerParaAlterarCamposRestritos() {
        var created = workspaces.create(workspaceFactory.adminWithAuthorizerGroup("Workspace Owner Fields", "TEAM_A"))
                .expectCreated().extractBody();
        allowTeamAAdministrator();

        workspaces.update(created.identifier(), workspaceUpdateFactory.changingSensitiveFields(created, "MANAGER", "TEAM_A"))
                .expectForbidden();
        workspaces.update(created.identifier(), workspaceUpdateFactory.changingSensitiveFields(created, "ADMIN", "TEAM_B"))
                .expectForbidden();
        workspaces.update(created.identifier(), workspaceUpdateFactory.changingSensitiveFields(created, "ADMIN", null))
                .expectForbidden();

        allowOwner();
        workspaces.update(created.identifier(), workspaceUpdateFactory.changingSensitiveFields(created, "MANAGER", "TEAM_B"))
                .expectOk().expect("workspaceType", "MANAGER").expect("authorizerGroup", "TEAM_B");
    }

    @Test
    @DisplayName("AUDIT - registra a atualização do workspace")
    void registraAuditoriaDaAtualizacao() {
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER").userName("auditor")
                .accountId("audit-account").applicationId("audit-application")
                .environmentId("audit-environment").traceId("audit-trace"));
        var created = workspaces.create(workspaceFactory.admin("Workspace Audit Update"))
                .expectCreated().extractBody();
        clearInvocations(auditAppender);

        workspaces.update(created.identifier(), workspaceUpdateFactory.renamed(created, "Workspace Audit Updated")).expectOk();

        ArgumentCaptor<JsonNode> payload = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<JsonNode> metadata = ArgumentCaptor.forClass(JsonNode.class);
        verify(auditAppender).append(payload.capture(), metadata.capture());
        assertEquals("WORKSPACE_UPDATED", metadata.getValue().path("eventType").asText());
        assertEquals(created.identifier(), metadata.getValue().path("resourceIdentifier").asText());
        assertEquals("auditor", metadata.getValue().path("username").asText());
        assertEquals("audit-trace", metadata.getValue().path("correlationId").asText());
        assertEquals("Workspace Audit Updated", payload.getValue().path("name").asText());
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

    private List<WorkspaceResponse> concurrently(
            java.util.concurrent.Callable<WorkspaceResponse> first,
            java.util.concurrent.Callable<WorkspaceResponse> second) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var firstResponse = executor.submit(first);
            var secondResponse = executor.submit(second);
            return List.of(firstResponse.get(30, TimeUnit.SECONDS), secondResponse.get(30, TimeUnit.SECONDS));
        }
    }
}
