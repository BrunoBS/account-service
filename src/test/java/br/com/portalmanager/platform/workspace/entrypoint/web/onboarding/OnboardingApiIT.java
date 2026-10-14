package br.com.portalmanager.platform.workspace.entrypoint.web.onboarding;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class OnboardingApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuthorizationMock authorization;

    @DynamicPropertySource
    static void auditProperties(DynamicPropertyRegistry registry) {
        registry.add("platform.audit.enabled", () -> true);
        registry.add("platform.audit.service-name", () -> "workspace-service");
    }

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 'Management workspace', 2, true, '{}')"
        );
        jdbc.update("INSERT IGNORE INTO type_application_scopes (code,label,sort_order,is_active,settings) VALUES ('BACKEND','Backend',1,true,'{}')");
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER").userName("onb-owner"));
    }

    @Test
    void completesConcurrentRequestsOnlyOnceWithoutAudit() throws Exception {
        String identifier = createWorkspace();
        seedRemainingCompletions(identifier);
        Long originalVersion = workspaceVersion(identifier);
        Long originalAuditCount = auditCount();
        List<Map<String, Object>> history = completionHistory(identifier);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> completeConcurrently(identifier, ready, start));
            Future<Integer> second = executor.submit(() -> completeConcurrently(identifier, ready, start));
            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            } finally {
                start.countDown();
            }
            assertThat(first.get(20, TimeUnit.SECONDS)).isEqualTo(204);
            assertThat(second.get(20, TimeUnit.SECONDS)).isEqualTo(204);
        }
        assertThat(workspaceVersion(identifier)).isEqualTo(originalVersion + 1);
        assertThat(completionHistory(identifier)).isEqualTo(history);
        assertThat(auditCount()).isEqualTo(originalAuditCount);
    }

    @Test
    void returnsLocalizedConfigurationErrorsAndKeepsTheWorkspaceIncomplete() {
        String identifier = createWorkspace();
        jdbc.update("UPDATE type_onboardings SET is_active = false WHERE code = 'APPLICATION_FIRST_ENVIRONMENT'");

        authorized().header("Accept-Language", "pt-BR").get(onboardingPath(identifier)).then().statusCode(500)
            .body("code", equalTo("ONBOARDING-0002"))
            .body("message", equalTo("O catálogo de fases do onboarding está configurado incorretamente."));
        authorized().patch(onboardingPath(identifier)).then().statusCode(500)
            .body("code", equalTo("ONBOARDING-0002"));
        assertThat(jdbc.queryForObject("SELECT onboarding FROM workspaces WHERE identifier = ?",
            Boolean.class, identifier)).isFalse();
    }

    @Test
    void returnsTheOnboardingMessageForInvalidStoredSettings() {
        String identifier = createWorkspace();
        jdbc.update("UPDATE type_onboardings SET settings = '{\"orientation\":123}' WHERE code = 'WORKSPACE_FIRST_ENVIRONMENT'");

        authorized().get(onboardingPath(identifier)).then().statusCode(500)
            .body("code", equalTo("ONBOARDING-0005"));
    }

    @Test
    void rollsBackFirstApplicationAndTagsWhenRequesterIsUnavailable() {
        String identifier = createWorkspace();
        Long originalAuditCount = auditCount();
        List<Map<String, Object>> history = completionHistory(identifier);
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER").userName(" "));

        authorized().contentType(ContentType.JSON).body(Map.of(
            "name", "First application", "alias", "first-app", "acronym", "APP", "applicationScope", "BACKEND",
            "settings", Map.of("language", "JAVA", "infrastructure", "VM"), "tags", List.of("manual-tag")
        )).post("/api/v1/workspaces/" + identifier + "/applications").then().statusCode(500)
            .body("code", equalTo("ONBOARDING-0004"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM applications", Long.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM application_tag", Long.class)).isZero();
        assertThat(completionHistory(identifier)).isEqualTo(history);
        assertThat(auditCount()).isEqualTo(originalAuditCount);
    }

    @Test
    void recordsAuthenticatedRequesterAtTheMaximumLength() {
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER").userName("1234567890"));
        String identifier = createWorkspace();

        authorized().get(onboardingPath(identifier)).then().statusCode(200)
            .body("phases[0].requester", equalTo("1234567890"));
        assertThat(completionHistory(identifier).getFirst().get("requester")).isEqualTo("1234567890");
    }

    @Test
    void rejectsLongRequesterAndRollsBackWorkspaceCreation() {
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER").userName("12345678901"));
        Long originalWorkspaceCount = jdbc.queryForObject("SELECT COUNT(*) FROM workspaces", Long.class);
        Long originalAuditCount = auditCount();

        authorized().contentType(ContentType.JSON).body(workspaceRequest())
            .post("/api/v1/workspaces").then().statusCode(500)
            .body("code", equalTo("ONBOARDING-0006"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM workspaces", Long.class))
            .isEqualTo(originalWorkspaceCount);
        assertThat(auditCount()).isEqualTo(originalAuditCount);
    }

    @Test
    void completesWithoutAuditAndPreservesHistoryOnRepeatedRequests() {
        String identifier = createWorkspace();
        seedRemainingCompletions(identifier);
        String path = onboardingPath(identifier);
        List<Map<String, Object>> history = completionHistory(identifier);
        Long originalVersion = workspaceVersion(identifier);
        Long originalAuditCount = auditCount();

        authorized().get(path).then().statusCode(200)
            .body("completed", equalTo(false)).body("readyToComplete", equalTo(true));
        authorized().patch(path).then().statusCode(204);
        authorized().get(path).then().statusCode(200)
            .body("completed", equalTo(true)).body("readyToComplete", equalTo(true));
        assertThat(workspaceVersion(identifier)).isEqualTo(originalVersion + 1);
        assertThat(auditCount()).isEqualTo(originalAuditCount);

        authorized().patch(path).then().statusCode(204);
        assertThat(workspaceVersion(identifier)).isEqualTo(originalVersion + 1);
        assertThat(completionHistory(identifier)).isEqualTo(history);
        assertThat(auditCount()).isEqualTo(originalAuditCount);
    }

    @Test
    void rejectsIncompleteProgressWithoutChangingWorkspaceOrHistory() {
        String identifier = createWorkspace();
        List<Map<String, Object>> history = completionHistory(identifier);
        Long originalVersion = workspaceVersion(identifier);
        Long originalAuditCount = auditCount();

        authorized().patch(onboardingPath(identifier)).then().statusCode(400)
            .body("code", equalTo("ONBOARDING-0003"));
        authorized().get(onboardingPath(identifier)).then().statusCode(200)
            .body("completed", equalTo(false)).body("readyToComplete", equalTo(false));
        assertThat(workspaceVersion(identifier)).isEqualTo(originalVersion);
        assertThat(completionHistory(identifier)).isEqualTo(history);
        assertThat(auditCount()).isEqualTo(originalAuditCount);
    }

    @Test
    void appliesWorkspaceVisibilityToProgressAndCompletion() {
        String identifier = createWorkspace();
        seedRemainingCompletions(identifier);
        authorization.reset();
        authorization.allow(session -> session.groups("USER")
            .addAuthorizerGroup("GRP_WORKSPACE_DEV_OTHER_TEAM", "DEV", "DEV", "OTHER_TEAM"));

        authorized().get(onboardingPath(identifier)).then().statusCode(404);
        authorized().patch(onboardingPath(identifier)).then().statusCode(404);
        assertThat(jdbc.queryForObject("SELECT onboarding FROM workspaces WHERE identifier = ?",
            Boolean.class, identifier)).isFalse();

        authorization.reset();
        authorization.allow(session -> session.groups("USER")
            .addAuthorizerGroup("GRP_WORKSPACE_DEV_ONB_TEAM", "DEV", "DEV", "ONB_TEAM"));
        authorized().get(onboardingPath(identifier)).then().statusCode(200);
        authorized().patch(onboardingPath(identifier)).then().statusCode(204);
    }

    @Test
    void rejectsInactiveWorkspacesForProgressAndCompletion() {
        String identifier = createWorkspace();
        seedRemainingCompletions(identifier);
        authorized().post("/api/v1/workspaces/" + identifier + "/inactivate").then().statusCode(204);

        authorized().get(onboardingPath(identifier)).then().statusCode(404);
        authorized().patch(onboardingPath(identifier)).then().statusCode(404);
        assertThat(jdbc.queryForObject("SELECT onboarding FROM workspaces WHERE identifier = ?",
            Boolean.class, identifier)).isFalse();
    }

    private String createWorkspace() {
        return authorized().contentType(ContentType.JSON).body(workspaceRequest())
            .post("/api/v1/workspaces").then().statusCode(201).extract().path("identifier");
    }

    private int completeConcurrently(String identifier, CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent completion did not start");
        return authorized().patch(onboardingPath(identifier)).statusCode();
    }

    private Map<String, Object> workspaceRequest() {
        return Map.of(
            "workspaceType", "MANAGER",
            "name", "Onboarding " + UUID.randomUUID(),
            "description", "Workspace para revisão de onboarding",
            "requester", "requester",
            "acronym", "ONB",
            "authorizerGroup", "ONB_TEAM",
            "settings", Map.of(),
            "emailGroup", "workspace@portalmanager.com",
            "approvers", List.of(Map.of("functional", "F1234", "email", "approver@portalmanager.com"))
        );
    }

    // Environment configuration hooks are not available yet; seed only their persisted result.
    private void seedRemainingCompletions(String identifier) {
        jdbc.update("""
            INSERT INTO workspace_onboarding_completions
                (workspace_id, onboarding_phase_code, requester, completed_at)
            SELECT w.id, p.code, 'phase-user', '2026-10-01 12:00:00'
            FROM workspaces w CROSS JOIN type_onboardings p
            WHERE w.identifier = ?
              AND p.code IN ('WORKSPACE_FIRST_ENVIRONMENT', 'FIRST_APPLICATION_REGISTRATION',
                             'APPLICATION_FIRST_ENVIRONMENT')
            """, identifier);
    }

    private List<Map<String, Object>> completionHistory(String identifier) {
        return jdbc.queryForList("""
            SELECT c.onboarding_phase_code, c.requester, c.completed_at
            FROM workspace_onboarding_completions c JOIN workspaces w ON w.id = c.workspace_id
            WHERE w.identifier = ? ORDER BY c.onboarding_phase_code
            """, identifier);
    }

    private Long workspaceVersion(String identifier) {
        return jdbc.queryForObject("SELECT version FROM workspaces WHERE identifier = ?", Long.class, identifier);
    }

    private Long auditCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Long.class);
    }

    private String onboardingPath(String identifier) {
        return "/api/v1/workspaces/" + identifier + "/onboarding";
    }

    private RequestSpecification authorized() {
        return given().port(port).header("Authorization", "Bearer onboarding-api-it")
            .header("correlation-id", "onboarding-api-it").accept(ContentType.JSON);
    }
}
