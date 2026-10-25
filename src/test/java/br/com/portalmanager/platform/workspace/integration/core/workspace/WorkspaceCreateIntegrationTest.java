package br.com.portalmanager.platform.workspace.integration.core.workspace;


import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceFactory.InvalidWorkspace;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceClient;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceFactory;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceProtocolPayloads;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceResponse;
import br.com.portalmanager.platform.workspace.integration.core.workspace.support.WorkspaceScenario;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationClient;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationFactory;
import br.com.portalmanager.platform.workspace.integration.core.application.support.ApplicationScenario;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.applicationscopetype.support.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.integration.feature.onboarding.support.OnboardingClient;


import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingPhaseFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.onboardingphasetype.support.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.integration.foundation.catalog.workspacetype.support.WorkspaceTypeFactory;
import br.com.portalmanager.platform.workspace.integration.foundation.schema.support.SchemaScenario;
import br.com.portalmanager.platform.workspace.integration.support.BaseIntegrationTest;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
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
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@DisplayName("Integração HTTP - Workspace Create")
@WithMySql(excludeTables = {
        "flyway_schema_history",
        "type_life_cycle",
        "type_schema_scopes",
        "type_schema_version_status"
})
class WorkspaceCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private WorkspaceTypeClient workspaceTypes;
    @Autowired private OnboardingTypeClient onboardingTypes;
    @Autowired private WorkspaceClient workspaces;
    @Autowired private SchemaScenario schemaScenario;
    @Autowired private ApplicationScopeTypeClient applicationScopes;
    @Autowired private ApplicationClient applications;
    @Autowired private OnboardingClient onboarding;

    private final WorkspaceTypeFactory workspaceTypeFactory = new WorkspaceTypeFactory();
    private final OnboardingPhaseFactory onboardingPhaseFactory = new OnboardingPhaseFactory();
    private final WorkspaceFactory workspaceFactory = new WorkspaceFactory();
    private final ApplicationFactory applicationFactory = new ApplicationFactory();

    @BeforeEach
    void prepareWorkspaceScenario() {
        new WorkspaceScenario(authorization, workspaceTypes, workspaceTypeFactory,
                onboardingTypes, onboardingPhaseFactory, schemaScenario).setup();
    }

    @Test
    @DisplayName("CREATE - cria e consulta um workspace ativo")
    void criaEBuscaWorkspaceAtivo() {
        var created = workspaces.create(workspaceFactory.valid())
                .expectCreated()
                .extractBody();
        workspaces.findById(created.identifier())
                .expectOk()
                .expect("identifier", created.identifier())
                .expectContains("name", "Workspace")
                .expect("lifecycle", "ACTIVE");
    }

    @Test
    @DisplayName("ONBOARDING - cria workspace e conclui a fase ao registrar a primeira aplicação")
    void concluiFaseDeOnboardingAoRegistrarPrimeiraAplicacao() {
        var workspace = workspaces.create(workspaceFactory.manager("Workspace onboarding"))
                .expectCreated()
                .extractBody();
        onboarding.findProgress(workspace.identifier()).expectOk()
                .expect("phases[0].code", "WORKSPACE_REGISTRATION")
                .expect("phases[0].status", "COMPLETED");

        new ApplicationScenario(schemaScenario, applicationScopes).setup();

        onboarding.findProgress(workspace.identifier()).expectOk()
                .expect("phases[0].code", "WORKSPACE_REGISTRATION")
                .expect("phases[0].status", "COMPLETED");

        applications.create(workspace.identifier(), applicationFactory.valid("First application"))
                .expectCreated();


        onboarding.findProgress(workspace.identifier()).expectOk()
                .expect("phases[0].code", "WORKSPACE_REGISTRATION")
                .expect("phases[0].status", "COMPLETED")
                .expect("phases[2].code", "FIRST_APPLICATION_REGISTRATION")
                .expect("phases[2].status", "COMPLETED");
    }

    @Test
    @DisplayName("VALIDATION - rejeita workspace sem nome")
    void rejeitaWorkspaceSemNome() {
        workspaces.create(workspaceFactory.withoutName())
                .expectBadRequest()
                .expect("details.field", List.of("name"));
    }

    @Test
    @DisplayName("VALIDATION - rejeita workspace sem tipo")
    void rejeitaWorkspaceSemTipo() {
        workspaces.create(workspaceFactory.withoutWorkspaceType())
                .expectBadRequest()
                .expectContainsItem("details.field", "workspaceType");
    }

    @Test
    @DisplayName("VALIDATION - rejeita workspace sem descrição")
    void rejeitaWorkspaceSemDescricao() {
        workspaces.create(workspaceFactory.withoutDescription())
                .expectBadRequest()
                .expectContainsItem("details.field", "description");
    }

    @Test
    @DisplayName("VALIDATION - rejeita workspace sem solicitante")
    void rejeitaWorkspaceSemSolicitante() {
        workspaces.create(workspaceFactory.withoutRequester())
                .expectBadRequest()
                .expectContainsItem("details.field", "requester");
    }

    @Test
    @DisplayName("VALIDATION - rejeita workspace sem sigla")
    void rejeitaWorkspaceSemSigla() {
        workspaces.create(workspaceFactory.withoutAcronym())
                .expectBadRequest()
                .expectContainsItem("details.field", "acronym");
    }

    @Test
    @DisplayName("VALIDATION - rejeita workspace sem e-mail do grupo")
    void rejeitaWorkspaceSemEmailDoGrupo() {
        workspaces.create(workspaceFactory.withoutEmailGroup())
                .expectBadRequest()
                .expectContainsItem("details.field", "emailGroup");
    }

    @Test
    @DisplayName("VALIDATION - rejeita workspace sem aprovadores")
    void rejeitaWorkspaceSemAprovadores() {
        workspaces.create(workspaceFactory.withoutApprovers())
                .expectBadRequest()
                .expectContainsItem("details.field", "approvers");
    }

    @Test
    @DisplayName("VALIDATION - rejeita aprovador sem funcional")
    void rejeitaAprovadorSemFuncao() {
        workspaces.create(workspaceFactory.withoutApproverFunctional())
                .expectBadRequest()
                .expectContainsItem("details.field", "approvers[0].functional");
    }

    @Test
    @DisplayName("VALIDATION - rejeita aprovador sem e-mail")
    void rejeitaAprovadorSemEmail() {
        workspaces.create(workspaceFactory.withoutApproverEmail())
                .expectBadRequest()
                .expectContainsItem("details.field", "approvers[0].email");
    }

    @Test
    @DisplayName("VALIDATION - rejeita e-mail malformado no aprovador")
    void rejeitaEmailMalformadoNoAprovador() {
        workspaces.create(workspaceFactory.withInvalidApproverEmail())
                .expectBadRequest()
                .expectContainsItem("details.field", "approvers[0].email");
    }

    @Test
    @DisplayName("VALIDATION - rejeita campos obrigatórios compostos só por espaços")
    void rejeitaCamposObrigatoriosEmBranco() {
        for (WorkspaceFactory.InvalidWorkspace invalid : workspaceFactory.blankRequiredFields()) {
            workspaces.create(invalid.request())
                    .expectBadRequest()
                    .expectContainsItem("details.field", invalid.field());
        }
    }

    @Test
    @DisplayName("VALIDATION - rejeita tipo de workspace cadastrado mas inativo")
    void rejeitaTipoDeWorkspaceInativo() {
        workspaceTypes.delete("ADMIN").expectNoContent();

        workspaces.create(workspaceFactory.admin("Workspace Inactive Type"))
                .expectBadRequest()
                .expectContainsItem("details.field", "workspaceType");
    }

    @Test
    @DisplayName("VALIDATION - aceita os comprimentos mínimos dos campos obrigatórios")
    void aceitaComprimentosMinimos() {
        var created = workspaces.create(workspaceFactory.minimumLengths())
                .expectCreated()
                .extractBody();

        workspaces.findById(created.identifier()).expectOk()
                .expect("name", "Min")
                .expect("description", "1234567890")
                .expect("requester", "12345")
                .expect("acronym", "A")
                .expect("emailGroup", "a@b.com")
                .expect("approvers[0].functional", "F")
                .expect("approvers[0].email", "c@d.com");
    }

    @Test
    @DisplayName("VALIDATION - aceita o máximo de aprovadores e tags")
    void aceitaMaximoDeAprovadoresETags() {
        var created = workspaces.create(workspaceFactory.maximumCollectionSizes())
                .expectCreated()
                .extractBody();

        workspaces.findById(created.identifier()).expectOk()
                .expectSize("approvers", 5)
                .expectSize("tags", 20)
                .expect("approvers[4].functional", "F4")
                .expect("approvers[4].email", "user4@example.com")
                .expectContainsItem("tags", "tag-19");
    }

    @Test
    @DisplayName("VALIDATION - aceita configurações nulas e normaliza tags nulas")
    void aceitaConfiguracoesETagsNulas() {
        var created = workspaces.create(workspaceFactory.withNullSettingsAndTags())
                .expectCreated()
                .extractBody();

        workspaces.findById(created.identifier()).expectOk()
                .expect("settings", null)
                .expectSize("tags", 0);
    }

    @Test
    @DisplayName("VALIDATION - não permite informar versão na criação")
    void rejeitaVersaoInformadaNaCriacao() {
        workspaces.create(workspaceFactory.withVersionOnCreate())
                .expectBadRequest()
                .expectContainsItem("details.field", "version");
    }

    @Test
    @DisplayName("VALIDATION - rejeita valores acima dos limites dos campos na criação")
    void rejeitaValoresAcimaDosLimitesNaCriacao() {
        for (WorkspaceFactory.InvalidWorkspace invalid : workspaceFactory.fieldsAboveMaximumLength()) {
            workspaces.create(invalid.request())
                    .expectBadRequest()
                    .expectContainsItem("details.field", invalid.field());
        }
    }

    @Test
    @DisplayName("VALIDATION - rejeita mais de cinco aprovadores ou vinte tags")
    void rejeitaColecoesAcimaDosLimitesNaCriacao() {
        workspaces.create(workspaceFactory.withSixApprovers())
                .expectBadRequest()
                .expectContainsItem("details.field", "approvers");
        workspaces.create(workspaceFactory.withTwentyOneTags())
                .expectBadRequest()
                .expectContainsItem("details.field", "tags");
    }

    @Test
    @DisplayName("VALIDATION - rejeita aprovadores com função ou e-mail duplicado")
    void rejeitaAprovadoresDuplicadosNaCriacao() {
        workspaces.create(workspaceFactory.withDuplicateApproverFunctionals())
                .expectBadRequest()
                .expect("code", "GLOBAL-0001")
                .expectContainsItem("details.field", "approvers[1].functional");

        workspaces.create(workspaceFactory.withDuplicateApproverEmails())
                .expectBadRequest()
                .expect("code", "GLOBAL-0001")
                .expectContainsItem("details.field", "approvers[1].email");
    }

    @Test
    @DisplayName("CREATE - preserva as configurações informadas")
    void preservaConfiguracoesInformadas() throws Exception {
        var settings = JsonMapper.builder().build()
                .readTree("{\"region\":\"south\",\"flags\":{\"enabled\":true}}");
        var created = workspaces.create(workspaceFactory.withSettings(settings))
                .expectCreated()
                .extractBody();

        workspaces.findById(created.identifier()).expectOk()
                .expect("settings.region", "south")
                .expect("settings.flags.enabled", true);
    }

    @Test
    @DisplayName("VALIDATION - retorna os campos inválidos do payload")
    void retornaCamposInvalidosDoPayload() {
        workspaces.create(workspaceFactory.withInvalidRequiredFields())
                .expectBadRequest()
                .expect("code", "GLOBAL-0001")
                .expectContainsItem("details.field", "workspaceType")
                .expectContainsItem("details.field", "name")
                .expectContainsItem("details.field", "description")
                .expectContainsItem("details.field", "requester")
                .expectContainsItem("details.field", "acronym")
                .expectContainsItem("details.field", "emailGroup")
                .expectContainsItem("details.field", "approvers");
    }

    @Test
    @DisplayName("VALIDATION - rejeita estrutura incompatível sem coerção ou descarte")
    void rejeitaEstruturaIncompativel() {
        for (String invalidJson : WorkspaceProtocolPayloads.incompatibleCreateShapes()) {
            workspaces.createProtocolRequest(invalidJson)
                    .expectBadRequest()
                    .expect("code", "GLOBAL-0001");
        }
        workspaces.createMalformedJson("[").expectBadRequest();
    }

    @Test
    @DisplayName("VALIDATION - rejeita nomes duplicados sem diferenciar caixa e espaços")
    void rejeitaNomesDuplicadosNormalizados() {
        workspaces.create(workspaceFactory.admin("Workspace Único"))
                .expectCreated();
        workspaces.create(workspaceFactory.withNameContainingSpaces("  workspace único  "))
                .expectBadRequest()
                .expect("code", "GLOBAL-0001")
                .expectContainsItem("details.field", "name");
    }

    @Test
    @DisplayName("VALIDATION - mantém nomes únicos entre grupos e após inativação")
    void mantemUnicidadeGlobalDoNomeNaCriacao() {
        var original = workspaces.create(workspaceFactory.adminWithAuthorizerGroup(
                        "Global Workspace Name", "TEAM_A"))
                .expectCreated()
                .extractBody();

        workspaces.create(workspaceFactory.managerWithAuthorizerGroup(original.name(), "TEAM_B"))
                .expectBadRequest()
                .expectContainsItem("details.field", "name");

        workspaces.inactivate(original.identifier()).expectNoContent();
        workspaces.create(workspaceFactory.managerWithAuthorizerGroup(original.name(), "TEAM_B"))
                .expectBadRequest()
                .expectContainsItem("details.field", "name");
    }

    @Test
    @DisplayName("VALIDATION - rejeita tipo de catálogo como tipo de workspace")
    void rejeitaTipoDeCatalogoComoWorkspace() {
        workspaces.create(workspaceFactory.withUnsupportedWorkspaceType())
                .expectBadRequest();
    }

    @Test
    @DisplayName("AUTH - nega criação com grupo ou tipo ADMIN para usuário comum")
    void negaCriacaoDeAdminParaUsuarioSemPermissao() {
        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("USER"));

        workspaces.create(workspaceFactory.managerWithAuthorizerGroup("Restricted Manager", "TEAM_A"))
                .expectForbidden()
                .expect("code", "WORKSPACE-0120");

        workspaces.create(workspaceFactory.admin("Restricted Admin"))
                .expectForbidden()
                .expect("code", "WORKSPACE-0120");
    }

    @Test
    @DisplayName("AUTH - permite MANAGER sem grupo para usuário comum")
    void permiteManagerSemGrupoParaUsuarioComum() {
        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("USER"));

        workspaces.create(workspaceFactory.manager("Restricted Manager"))
                .expectCreated()
                .expect("workspaceType", "MANAGER")
                .expect("authorizerGroup", null);

        workspaces.create(workspaceFactory.managerWithBlankAuthorizerGroup("Blank Group Manager"))
                .expectCreated()
                .expect("authorizerGroup", null);
    }

    @Test
    @DisplayName("AUTH - permite criar ADMIN e MANAGER com grupo para owner")
    void permiteCriarTiposComGrupoParaOwner() {
        authorization.reset();
        authorization.allow(session -> session.userName("owner-it").groups("PM5_OWNER"));

        workspaces.create(workspaceFactory.adminWithAuthorizerGroup("Owner Admin", "TEAM_A"))
                .expectCreated()
                .expect("authorizerGroup", "TEAM_A");

        workspaces.create(workspaceFactory.managerWithAuthorizerGroup("Owner Manager", "TEAM_A"))
                .expectCreated()
                .expect("workspaceType", "MANAGER");
    }

    @Test
    @DisplayName("AUTH - aplica a política OPEN na criação")
    void aplicaPoliticaOpenNaCriacao() {
        workspaces.create(workspaceFactory.valid()).expectCreated();
        authorization.verifyCalledWithPolicy("OPEN");
    }

    @Test
    @DisplayName("CONCURRENCY - traduz colisão concorrente de nomes sem vazar detalhes do banco")
    void traduzColisaoConcorrenteDeCriacao() throws Exception {
        String name = "Concurrent Workspace Create";
        CyclicBarrier nameChecks = new CyclicBarrier(2);
        doAnswer(invocation -> {
            nameChecks.await(15, TimeUnit.SECONDS);
            return false;
        }).when(workspaceRepository).existsByName(matches("(?i)" + name));

        List<WorkspaceResponse> responses = concurrently(
                () -> workspaces.create(workspaceFactory.withConcurrentName(name)),
                () -> workspaces.create(workspaceFactory.withConcurrentName(name.toLowerCase(Locale.ROOT))));
        assertEquals(List.of(201, 409), responses.stream().map(WorkspaceResponse::statusCode).sorted().toList());

        WorkspaceResponse rejected = responses.stream()
                .filter(response -> response.statusCode() == 409)
                .findFirst().orElseThrow();
        rejected.expect("code", "GLOBAL-0004")
                .expectNotNull("message")
                .expectSize("details", 0);

        for (String internalDetail : List.of("uk_workspaces_name", "Duplicate entry", "ConstraintViolationException",
                "DataIntegrityViolationException", "workspace_approvers", "Concurrent Workspace Create")) {
            rejected.expectBodyDoesNotContain(internalDetail);
        }

        workspaces.findAll().execute().expectOk().expect("page.totalElements", 1);
    }

    @Test
    @DisplayName("SMOKE - normaliza campos e retorna o contrato do workspace ativo")
    void normalizaECriaWorkspaceAtivo() {
        var created = workspaces.create(workspaceFactory.withNormalizedFields())
                .expectCreated().extractBody();
        workspaces.findById(created.identifier()).expectOk()
                .expect("identifier", created.identifier())
                .expect("name", "Workspace G3")
                .expect("workspaceType", "ADMIN")
                .expect("description", "Descrição válida do workspace G3")
                .expect("requester", "requester")
                .expect("acronym", "G3")
                .expect("authorizerGroup", "GRP_WORK")
                .expect("emailGroup", "workspace@portalmanager.com")
                .expect("approvers[0].functional", "F1234")
                .expect("lifecycle", "ACTIVE")
                .expect("onboarding", false);
    }

    @Test
    @DisplayName("TAG - normaliza tags manuais e permite buscar também pelas tags do sistema")
    void normalizaTagsManuaisEBuscaTagsDoSistema() {
        var created = workspaces.create(workspaceFactory.withManualTagsForNormalization())
                .expectCreated().extractBody();
        workspaces.findById(created.identifier()).expectOk()
                .expectContainsItem("tags", "minha-tag")
                .expectContainsItem("tags", "outra-tag")
                .expectNotContainsItem("tags", "workspace-tags");

        workspaces.findAll().tagName("Minha Tag").execute().expectOk()
                .expectContainsItem("content.identifier", created.identifier());
        workspaces.findAll().tagName("Workspace Tags").execute().expectOk()
                .expectContainsItem("content.identifier", created.identifier());
        workspaces.findAll().tagName("TEAM_A").execute().expectOk()
                .expectContainsItem("content.identifier", created.identifier());
    }

    @Test
    @DisplayName("VALIDATION - aceita os limites máximos de tamanho")
    void aceitaComprimentosMaximos() {
        String maximumEmail = "a".repeat(63) + "@" + "b".repeat(32) + ".com";
        var created = workspaces.create(workspaceFactory.maximumLengths())
                .expectCreated()
                .extractBody();

        workspaces.findById(created.identifier()).expectOk()
                .expect("name", "N".repeat(50))
                .expect("description", "D".repeat(200))
                .expect("requester", "R".repeat(10))
                .expect("acronym", "ABCDE")
                .expect("authorizerGroup", "G".repeat(10))
                .expect("emailGroup", maximumEmail)
                .expectContainsItem("tags", "t".repeat(50))
                .expect("approvers[0].functional", "F".repeat(10))
                .expect("approvers[0].email", maximumEmail);
    }

    @Test
    @DisplayName("AUDIT - registra a criação do workspace")
    void registraAuditoriaDaCriacao() {
        authorization.reset();
        authorization.allow(session ->
                session.groups("PM5_OWNER")
                        .userName("auditor")
                        .accountId("audit-account")
                        .applicationId("audit-application")
                        .environmentId("audit-environment")
                        .traceId("audit-trace"));
        clearInvocations(auditAppender);

        var created = workspaces.create(workspaceFactory.valid())
                .expectCreated().extractBody();

        ArgumentCaptor<JsonNode> payload = ArgumentCaptor.forClass(JsonNode.class);
        ArgumentCaptor<JsonNode> metadata = ArgumentCaptor.forClass(JsonNode.class);
        verify(auditAppender).append(payload.capture(), metadata.capture());
        assertEquals("WORKSPACE_CREATED", metadata.getValue().path("eventType").asText());
        assertEquals("WORKSPACE", metadata.getValue().path("resourceType").asText());
        assertEquals(created.identifier(), metadata.getValue().path("resourceIdentifier").asText());
        assertEquals("auditor", metadata.getValue().path("username").asText());
        assertEquals("audit-trace", metadata.getValue().path("correlationId").asText());
        assertEquals(created.identifier(), payload.getValue().path("identifier").asText());
    }

    private List<WorkspaceResponse> concurrently(
            Callable<WorkspaceResponse> first,
            Callable<WorkspaceResponse> second) throws Exception {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var firstResponse = executor.submit(first);
            var secondResponse = executor.submit(second);
            return List.of(firstResponse.get(30, TimeUnit.SECONDS), secondResponse.get(30, TimeUnit.SECONDS));
        }
    }
}
