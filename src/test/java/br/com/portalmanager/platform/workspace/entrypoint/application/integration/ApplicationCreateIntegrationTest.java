package br.com.portalmanager.platform.workspace.entrypoint.application.integration;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.workspace.entrypoint.application.ApplicationClient;
import br.com.portalmanager.platform.workspace.entrypoint.application.ApplicationFactory;
import br.com.portalmanager.platform.workspace.entrypoint.application.ApplicationScenario;
import br.com.portalmanager.platform.workspace.entrypoint.onboarding.support.OnboardingClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.application_scope.ApplicationScopeTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.onboarding.OnboardingTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.catalog.workspace.WorkspaceTypeClient;
import br.com.portalmanager.platform.workspace.entrypoint.schema.SchemaScenario;
import br.com.portalmanager.platform.workspace.support.BaseIntegrationTest;
import br.com.portalmanager.platform.workspace.entrypoint.workspace.support.WorkspaceClient;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Integração HTTP - Application Create")
@WithMySql(excludeTables = {"flyway_schema_history", "type_life_cycle", "type_schema_scopes", "type_schema_version_status"})
class ApplicationCreateIntegrationTest extends BaseIntegrationTest {
    @Autowired private AuthorizationMock authorization;
    @Autowired private WorkspaceClient workspaces;
    @Autowired private WorkspaceTypeClient workspaceTypes;
    @Autowired private OnboardingTypeClient onboardingTypes;
    @Autowired private ApplicationScopeTypeClient applicationScopes;
    @Autowired private SchemaScenario schemas;
    @Autowired private ApplicationClient applications;
    @Autowired private OnboardingClient onboarding;

    private final ApplicationFactory factory = new ApplicationFactory();
    private ApplicationScenario scenario;

    @BeforeEach
    void prepareScenario() {
        scenario = new ApplicationScenario(authorization, workspaces, workspaceTypes, onboardingTypes,
                applicationScopes, schemas).setup();
    }

    @Test
    @DisplayName("CREATE - cria aplicação com campos normalizados e configurações estruturadas")
    void criaAplicacaoNormalizada() {
        var created = applications.create(scenario.workspaceIdentifier(), factory.withStructuredSettings())
                .expectCreated()
                .expect("name", "Application")
                .expect("applicationScope", "BACKEND")
                .expect("settings.language", "JAVA")
                .expect("lifecycle", "ACTIVE")
                .extractBody();

        applications.findByIdentifier(scenario.workspaceIdentifier(), created.identifier())
                .expectOk()
                .expect("settings.infrastructure", "VM")
                .expect("authorizerGroup", null);
    }

    @Test
    @DisplayName("VALIDATION - rejeita workspace que não é do tipo MANAGER")
    void rejeitaWorkspaceQueNaoEManager() {
        String admin = scenario.createAdminWorkspace("Administrative application workspace");
        applications.create(admin, factory.valid())
                .expectBadRequest()
                .expectContainsItem("details.field", "workspaceIdentifier");
    }

    @Test
    @DisplayName("VALIDATION - rejeita escopo de aplicação desconhecido e nome duplicado")
    void rejeitaEscopoInvalidoENomeDuplicado() {
        applications.create(scenario.workspaceIdentifier(), factory.withUnsupportedScope())
                .expectBadRequest()
                .expectContainsItem("details.field", "applicationScope");
        applications.create(scenario.workspaceIdentifier(), factory.valid("Duplicate application")).expectCreated();
        applications.create(scenario.workspaceIdentifier(), factory.withDuplicateName("Duplicate application"))
                .expectBadRequest()
                .expectContainsItem("details.field", "name");
    }

    @Test
    @DisplayName("VALIDATION - aceita limites máximos e rejeita valores acima dos limites")
    void validaLimitesDosCampos() {
        applications.create(scenario.workspaceIdentifier(), factory.withMaximumLengths()).expectCreated();

        for (var invalid : factory.fieldsAboveMaximumLength()) {
            applications.create(scenario.workspaceIdentifier(), invalid.request())
                    .expectBadRequest()
                    .expectContainsItem("details.field", invalid.field());
        }
        applications.create(scenario.workspaceIdentifier(), factory.withTooManyTags())
                .expectBadRequest()
                .expectContainsItem("details.field", "tags");
    }

    @Test
    @DisplayName("VALIDATION - aplica o schema original, normaliza tags e trata campos opcionais")
    void validaSchemaTagsECamposOpcionais() {
        applications.create(scenario.workspaceIdentifier(), factory.withDuplicateTags())
                .expectCreated()
                .expect("tags", List.of("managed"));

        applications.create(scenario.workspaceIdentifier(), factory.withEmptyTags())
                .expectCreated()
                .expectSize("tags", 0);

        applications.create(scenario.workspaceIdentifier(), factory.withNullSettings()).expectBadRequest();
        applications.create(scenario.workspaceIdentifier(), factory.withInvalidSettings()).expectBadRequest();
        applications.create(scenario.workspaceIdentifier(), factory.withoutAlias()).expectBadRequest();
    }

    @Test
    @DisplayName("AUTH - exige autorização sobre o workspace pai para criar aplicações")
    void exigeAutorizacaoDoWorkspacePai() {
        String workspace = scenario.createManagerWorkspace("Protected application workspace", "PARENT_A");
        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_ADM_OTHER", "ADM", "ADM", "OTHER"));

        applications.create(workspace, factory.valid()).expectNotFound().expect("code", "WORKSPACE-0001");
        authorization.verifyCalledWithPolicy("ADM");
        authorization.reset();

        authorization.allow(session -> session.userName("test-user").groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_ADM_PARENT_A", "ADM", "ADM", "PARENT_A"));

        applications.create(workspace, factory.valid()).expectCreated();
    }

    @Test
    @DisplayName("LIFECYCLE - impede criar aplicação em workspace inativo ou em quarentena")
    void impedeCriacaoEmWorkspaceIndisponivel() {
        String inactive = scenario.workspaceIdentifier();
        workspaces.inactivate(inactive).expectNoContent();

        applications.create(inactive, factory.valid())
                .expectNotFound()
                .expect("code", "WORKSPACE-0001");


        String quarantined = scenario.createManagerWorkspace("Quarantined application workspace", null);
        workspaces.inactivate(quarantined).expectNoContent();
        workspaces.delete(quarantined).expectNoContent();
        applications.create(quarantined, factory.valid())
                .expectNotFound()
                .expect("code", "WORKSPACE-0001");
    }

    @Test
    @DisplayName("ONBOARDING - registra a primeira aplicação uma única vez")
    void registraPrimeiraAplicacaoNoOnboarding() {
        applications.create(scenario.workspaceIdentifier(), factory.valid("First registered application")).expectCreated();
        onboarding.findProgress(scenario.workspaceIdentifier()).expectOk()
                .expect("phases[0].code", "WORKSPACE_REGISTRATION")
                .expect("phases[0].status", "COMPLETED")
                .expect("phases[2].code", "FIRST_APPLICATION_REGISTRATION")
                .expect("phases[2].status", "COMPLETED");

        applications.create(scenario.workspaceIdentifier(), factory.valid("Second registered application")).expectCreated();
        onboarding.findProgress(scenario.workspaceIdentifier()).expectOk()
                .expect("phases[0].code", "WORKSPACE_REGISTRATION")
                .expect("phases[0].status", "COMPLETED")
                .expect("phases[2].code", "FIRST_APPLICATION_REGISTRATION")
                .expect("phases[2].status", "COMPLETED");
    }

    @Test
    @DisplayName("CONCURRENCY - mantém único o nome de aplicações criadas simultaneamente")
    void mantemNomeUnicoDuranteCriacaoConcorrente() throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return applications.create(scenario.workspaceIdentifier(), factory.valid("Concurrent application name"));
            });
            var second = executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return applications.create(scenario.workspaceIdentifier(), factory.valid("Concurrent application name"));
            });
            int firstStatus = first.get(20, TimeUnit.SECONDS).statusCode();
            int secondStatus = second.get(20, TimeUnit.SECONDS).statusCode();
            assertTrue((firstStatus == 201 && (secondStatus == 400 || secondStatus == 409))
                    || (secondStatus == 201 && (firstStatus == 400 || firstStatus == 409)));
        }
        applications.findAll(scenario.workspaceIdentifier()).execute().expectOk()
                .expectSize("content", 1).expect("page.totalElements", 1);
    }

    @Test
    @DisplayName("CONCURRENCY - conclui o onboarding ao registrar duas primeiras aplicações simultâneas")
    void concluiOnboardingDuranteCriacaoConcorrente() throws Exception {
        var barrier = new CyclicBarrier(2);
        try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return applications.create(scenario.workspaceIdentifier(), factory.valid("Concurrent first application"));
            });
            var second = executor.submit(() -> {
                barrier.await(10, TimeUnit.SECONDS);
                return applications.create(scenario.workspaceIdentifier(), factory.valid("Concurrent second application"));
            });
            assertTrue(first.get(20, TimeUnit.SECONDS).statusCode() == 201);
            assertTrue(second.get(20, TimeUnit.SECONDS).statusCode() == 201);
        }
        applications.findAll(scenario.workspaceIdentifier()).execute().expectOk()
                .expectSize("content", 2).expect("page.totalElements", 2);
        onboarding.findProgress(scenario.workspaceIdentifier()).expectOk()
                .expect("phases[2].status", "COMPLETED");
    }
}
