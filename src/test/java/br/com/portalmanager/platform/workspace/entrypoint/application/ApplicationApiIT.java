package br.com.portalmanager.platform.workspace.entrypoint.application;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.LinkedHashMap;
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
import tools.jackson.databind.ObjectMapper;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class ApplicationApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuthorizationMock authorization;

    @Autowired
    private ObjectMapper json;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 'Management workspace', 2, true, '{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('ACTIVE', 'Active', 'Active lifecycle', 1, true, '{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('INACTIVE', 'Inactive', 'Inactive lifecycle', 2, true, '{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle', 3, true, '{}')"
        );
        jdbc.update(
            "INSERT IGNORE INTO type_application_scopes (code, label, description, sort_order, is_active, settings) VALUES ('BACKEND', 'Backend', 'Backend application', 1, true, '{}')"
        );
        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("PM5_OWNER"));
    }

    @Test
    void enforcesWorkspaceAlignedLimitsOnCreateAndUpdate() {
        String path = "/api/v1/workspaces/" + createWorkspace("MANAGER") + "/applications";
        Map<String, Object> payload = request("N".repeat(50));
        payload.put("alias", "A".repeat(50));
        payload.put("description", "D".repeat(200));
        payload.put("acronym", "ABCDE");
        payload.put("authorizerGroup", "A-" + "G".repeat(8));
        payload.put("tags", List.of("t".repeat(50)));
        ExtractableResponse<Response> created = post(path, payload).statusCode(201).extract();
        String identifier = created.path("identifier");
        assertThat(created.<String>path("description")).isEqualTo("D".repeat(200));
        payload.put("version", created.path("version"));
        payload.put("description", "E".repeat(200));
        put(path + "/" + identifier, payload)
            .statusCode(200)
            .body("description", equalTo("E".repeat(200)));
        get(path + "/" + identifier)
            .statusCode(200)
            .body("description", equalTo("E".repeat(200)));
        Map<String, String> excessiveValues = Map.of(
            "name",
            "N".repeat(51),
            "description",
            "D".repeat(201),
            "alias",
            "A".repeat(51),
            "acronym",
            "ABCDEF",
            "authorizerGroup",
            "A-" + "G".repeat(9)
        );
        for (Map.Entry<String, String> entry : excessiveValues.entrySet()) {
            Map<String, Object> invalid = request("Invalid Limit " + entry.getKey());
            invalid.put(entry.getKey(), entry.getValue());
            post(path, invalid).statusCode(400).body("details.field", hasItem(entry.getKey()));
            invalid.put(
                "version",
                get(path + "/" + identifier)
                    .extract()
                    .path("version")
            );
            put(path + "/" + identifier, invalid)
                .statusCode(400)
                .body("details.field", hasItem(entry.getKey()));
        }
        Map<String, Object> invalidTags = request("Invalid Tag Length");
        invalidTags.put("tags", List.of("t".repeat(51)));
        post(path, invalidTags).statusCode(400).body("details.field", hasItem("tags[0]"));
    }

    @Test
    void normalizesBeforeSchemaAndPreservesStructuredSettings() {
        String path = "/api/v1/workspaces/" + createWorkspace("MANAGER") + "/applications";
        Map<String, Object> payload = request("  Normalized Application  ");
        payload.put("applicationScope", " backend ");
        payload.put("settings", Map.of("language", "JAVA", "infrastructure", "VM"));
        String identifier = post(path, payload)
            .statusCode(201)
            .body("name", equalTo("Normalized Application"))
            .body("applicationScope", equalTo("BACKEND"))
            .body("settings.language", equalTo("JAVA"))
            .extract()
            .path("identifier");
        get(path + "/" + identifier)
            .statusCode(200)
            .body("settings.language", equalTo("JAVA"));
        Map<String, Object> invalid = request("Invalid Application");
        invalid.put("name", 123);
        post(path, invalid).statusCode(400);
        invalid = request("Unknown Field Application");
        invalid.put("unknown", true);
        post(path, invalid).statusCode(400);
        invalid = request("Too Many Tags Application");
        invalid.put(
            "tags",
            java.util.stream.IntStream.range(0, 21)
                .mapToObj(number -> "tag-" + number)
                .toList()
        );
        post(path, invalid).statusCode(400);
    }

    @Test
    void paginatesFiltersAndRejectsUnsupportedSorting() {
        String path = "/api/v1/workspaces/" + createWorkspace("MANAGER") + "/applications";
        post(path, request("Page First")).statusCode(201);
        post(path, request("Page Second")).statusCode(201);
        get(path + "?size=1&sort=name,asc")
            .statusCode(200)
            .body("content.name", org.hamcrest.Matchers.contains("Page First"))
            .body("page.totalElements", equalTo(2))
            .body("page.totalPages", equalTo(2));
        get(path + "?size=1&page=1&sort=name,asc")
            .statusCode(200)
            .body("content.name", org.hamcrest.Matchers.contains("Page Second"));
        get(path + "?sort=settings")
            .statusCode(400)
            .body("code", equalTo("APPLICATION-0109"));
    }

    @Test
    void createsUpdatesAndTransitionsThroughLifecycle() {
        String workspace = createWorkspace("MANAGER");
        String path = "/api/v1/workspaces/" + workspace + "/applications";
        String app = post(path, request("Portal App"))
            .statusCode(201)
            .body("applicationScope", equalTo("BACKEND"))
            .body("authorizerGroup", nullValue())
            .body("settings.language", equalTo("JAVA"))
            .body("lifecycle", equalTo("ACTIVE"))
            .extract()
            .path("identifier");

        Integer version = get(path + "/" + app)
            .statusCode(200)
            .extract()
            .path("version");
        Map<String, Object> update = request("Portal App Updated");
        update.put("version", version);
        put(path + "/" + app, update)
            .statusCode(200)
            .body("version", greaterThan(version));
        put(path + "/" + app, update).statusCode(409);

        get(path + "/summary")
            .statusCode(200)
            .body("identifier", hasItem(app));
        post(path + "/" + app + "/inactivate", null).statusCode(204);
        get(path + "/" + app).statusCode(404);
        get(path + "?lifecycle=INACTIVE")
            .statusCode(200)
            .body("content.identifier", hasItem(app));
        post(path + "/" + app + "/restore", null)
            .statusCode(200)
            .body("lifecycle", equalTo("ACTIVE"));
        delete(path + "/" + app).statusCode(400);
        post(path + "/" + app + "/inactivate", null).statusCode(204);
        delete(path + "/" + app).statusCode(204);
        get(path + "?lifecycle=INACTIVE")
            .statusCode(200)
            .body("content.identifier", not(hasItem(app)));
    }

    @Test
    void recordsOnboardingPhaseCompletionsOnceForManagerWorkspaceAndFirstApplication() {
        String workspace = createWorkspace("MANAGER");
        String onboardingPath = "/api/v1/workspaces/" + workspace + "/onboarding";

        assertThat(onboardingCompletionCount(workspace, "WORKSPACE_REGISTRATION")).isEqualTo(1);
        get(onboardingPath)
            .statusCode(200)
            .body("completed", equalTo(false))
            .body("readyToComplete", equalTo(false))
            .body("phases[0].code", equalTo("WORKSPACE_REGISTRATION"))
            .body("phases[0].status", equalTo("COMPLETED"))
            .body("phases[1].status", equalTo("IN_PROGRESS"));

        String applicationsPath = "/api/v1/workspaces/" + workspace + "/applications";
        String firstApplication = post(applicationsPath, request("First Onboarding Application"))
            .statusCode(201)
            .extract()
            .path("identifier");
        assertThat(onboardingCompletionCount(workspace, "FIRST_APPLICATION_REGISTRATION")).isEqualTo(1);

        post(applicationsPath + "/" + firstApplication + "/inactivate", null).statusCode(204);
        post(applicationsPath, request("Second Onboarding Application")).statusCode(201);
        assertThat(onboardingCompletionCount(workspace, "FIRST_APPLICATION_REGISTRATION")).isEqualTo(1);
        get(onboardingPath)
            .statusCode(200)
            .body("phases[2].status", equalTo("COMPLETED"))
            .body("phases[3].status", equalTo("PENDING"));
        patch(onboardingPath).statusCode(400).body("code", equalTo("ONBOARDING-0003"));

        String adminWorkspace = createWorkspace("ADMIN");
        get("/api/v1/workspaces/" + adminWorkspace + "/onboarding")
            .statusCode(400)
            .body("code", equalTo("ONBOARDING-0001"));
        assertThat(onboardingCompletionCount(adminWorkspace, "WORKSPACE_REGISTRATION")).isZero();
    }

    @Test
    void serializesConcurrentFirstApplicationPhaseCompletion() throws Exception {
        String workspace = createWorkspace("MANAGER");
        String applicationsPath = "/api/v1/workspaces/" + workspace + "/applications";
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> responses = List.of(
                executor.submit(() ->
                    createApplicationConcurrently(applicationsPath, "Concurrent App One", ready, start)
                ),
                executor.submit(() ->
                    createApplicationConcurrently(applicationsPath, "Concurrent App Two", ready, start)
                )
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(
                responses
                    .stream()
                    .map(response -> getStatus(response))
                    .toList()
            ).containsExactly(201, 201);
        }

        assertThat(onboardingCompletionCount(workspace, "FIRST_APPLICATION_REGISTRATION")).isEqualTo(1);
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM applications a JOIN workspaces w ON w.id=a.workspace_id WHERE w.identifier=?",
                Integer.class,
                workspace
            )
        ).isEqualTo(2);
    }

    @Test
    void keepsNamesUniqueDuringConcurrentCreation() throws Exception {
        String workspace = createWorkspace("MANAGER");
        String path = "/api/v1/workspaces/" + workspace + "/applications";
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() ->
                createApplicationConcurrently(path, "Concurrent Same Name", ready, start)
            );
            Future<Integer> second = executor.submit(() ->
                createApplicationConcurrently(path, "concurrent same name", ready, start)
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<Integer> statuses = List.of(getStatus(first), getStatus(second));
            assertThat(
                statuses
                    .stream()
                    .filter(status -> status == 201)
                    .count()
            ).isEqualTo(1);
            assertThat(
                statuses
                    .stream()
                    .filter(status -> status == 400 || status == 409)
                    .count()
            ).isEqualTo(1);
        }
        assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM applications WHERE name = 'Concurrent Same Name'", Integer.class)
        ).isEqualTo(1);
    }

    private Integer createApplicationConcurrently(
        String applicationsPath,
        String name,
        CountDownLatch ready,
        CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Concurrent test start timed out");
        return post(applicationsPath, request(name)).extract().statusCode();
    }

    private Integer getStatus(Future<Integer> response) {
        try {
            return response.get(20, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError("Concurrent application creation failed", exception);
        }
    }

    private int onboardingCompletionCount(String workspaceIdentifier, String phaseCode) {
        return jdbc.queryForObject(
            "SELECT COUNT(*) FROM workspace_onboarding_completions c JOIN workspaces w ON w.id=c.workspace_id WHERE w.identifier=? AND c.onboarding_phase_code=?",
            Integer.class,
            workspaceIdentifier,
            phaseCode
        );
    }

    @Test
    void rejectsNonManagerWorkspaceAndInvalidScope() {
        String admin = createWorkspace("ADMIN");
        post("/api/v1/workspaces/" + admin + "/applications", request("Admin App"))
            .statusCode(400)
            .body("details.field", hasItem("workspaceIdentifier"));

        String manager = createWorkspace("MANAGER");
        Map<String, Object> invalid = request("Invalid Scope");
        invalid.put("applicationScope", "UNKNOWN");
        post("/api/v1/workspaces/" + manager + "/applications", invalid)
            .statusCode(400)
            .body("details.field", hasItem("applicationScope"));
    }

    @Test
    void separatesManualAndSystemTagsAndReconcilesThemOnUpdateAndRestore() {
        String workspace = createWorkspace("MANAGER");
        String path = "/api/v1/workspaces/" + workspace + "/applications";
        Map<String, Object> create = request("Application Original");
        create.put("authorizerGroup", "TEAM_A");
        create.put("tags", List.of("minha-tag", "outra-tag"));
        ExtractableResponse<Response> created = post(path, create)
            .statusCode(201)
            .body("tags", containsInAnyOrder("minha-tag", "outra-tag"))
            .body("authorizerGroup", equalTo("A-TEAM_A"))
            .extract();
        String app = created.path("identifier");
        Integer version = created.path("version");

        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM application_tag t JOIN applications a ON a.id=t.application_id WHERE a.identifier=? AND t.origin_type='MANUAL'",
                Integer.class,
                app
            )
        ).isEqualTo(2);
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM application_tag t JOIN applications a ON a.id=t.application_id WHERE a.identifier=? AND t.origin_type='SYSTEM'",
                Integer.class,
                app
            )
        ).isGreaterThan(0);
        getByTag(path, "Minha Tag").statusCode(200).body("content.identifier", hasItem(app));
        getByTag(path, "A-TEAM_A").statusCode(200).body("content.identifier", hasItem(app));
        getByTag(path, "Application Original").statusCode(200).body("content.identifier", hasItem(app));

        Map<String, Object> update = request("Application Updated");
        update.put("version", version);
        update.put("authorizerGroup", "TEAM_B");
        update.put("tags", List.of("minha-tag"));
        put(path + "/" + app, update)
            .statusCode(200)
            .body("tags", contains("minha-tag"));
        getByTag(path, "Application Original")
            .statusCode(200)
            .body("content.identifier", not(hasItem(app)));
        getByTag(path, "A-TEAM_A")
            .statusCode(200)
            .body("content.identifier", not(hasItem(app)));
        getByTag(path, "A-TEAM_B").statusCode(200).body("content.identifier", hasItem(app));
        getByTag(path, "Outra Tag")
            .statusCode(200)
            .body("content.identifier", not(hasItem(app)));

        post(path + "/" + app + "/inactivate", null).statusCode(204);
        post(path + "/" + app + "/restore", null)
            .statusCode(200)
            .body("tags", contains("minha-tag"));
        getByTag(path, "Minha Tag").statusCode(200).body("content.identifier", hasItem(app));
    }

    @Test
    void validatesOriginalJsonBeforeBindingTypedRequests() {
        String path = "/api/v1/workspaces/" + createWorkspace("MANAGER") + "/applications";
        Map<String, Object> payload = request("Typed Application");
        ExtractableResponse<Response> created = post(path, payload).statusCode(201).extract();
        String identifier = created.path("identifier");
        Object version = created.path("version");
        Map<String, Object> invalidValues = Map.of(
            "name",
            123,
            "alias",
            true,
            "tags",
            List.of(123),
            "version",
            "0",
            "unexpected",
            "must not disappear",
            "settings",
            Map.of("language", "JAVA", "infrastructure", "VM", "unexpected", true)
        );
        for (Map.Entry<String, Object> invalidValue : invalidValues.entrySet()) {
            Map<String, Object> invalid = request("Invalid Typed Application");
            invalid.put("version", version);
            invalid.put(invalidValue.getKey(), invalidValue.getValue());
            post(path, invalid).statusCode(400);
            put(path + "/" + identifier, invalid).statusCode(400);
        }
        get(path + "/" + identifier)
            .statusCode(200)
            .body("version", equalTo(version));
    }

    @Test
    void validatesStructureAndDelegatesTagNormalization() {
        String workspace = createWorkspace("MANAGER");
        String path = "/api/v1/workspaces/" + workspace + "/applications";

        Map<String, Object> repeatedTags = request("Duplicate Tags");
        repeatedTags.put("tags", List.of("managed", "managed"));
        post(path, repeatedTags).statusCode(201).body("tags", contains("managed"));

        Map<String, Object> normalizedTag = request("Invalid Tag");
        normalizedTag.put("tags", List.of("Managed"));
        post(path, normalizedTag).statusCode(201).body("tags", contains("managed"));

        Map<String, Object> invalidSettings = request("Invalid Settings");
        invalidSettings.put("settings", "not-json-object");
        post(path, invalidSettings).statusCode(400);

        Map<String, Object> missingRequired = request("Missing Alias");
        missingRequired.remove("alias");
        post(path, missingRequired).statusCode(400);
    }

    @Test
    void validatesApplicationSettingsFields() {
        String path = "/api/v1/workspaces/" + createWorkspace("MANAGER") + "/applications";
        Map<String, Object> payload = request("Settings Validation");
        payload.put("settings", Map.of());
        post(path, payload).statusCode(400);
        payload.put("settings", Map.of("language", "PYTHON", "infrastructure", "VM"));
        post(path, payload).statusCode(400);
        payload.put("settings", Map.of("language", "JAVA", "infrastructure", "INVALID"));
        post(path, payload).statusCode(400);
        payload.put("settings", Map.of("language", "JAVA", "infrastructure", "EKS"));
        payload.put("settings", Map.of("language", "JAVA", "infrastructure", "EKS", "extra", true));
        post(path, payload).statusCode(400);
        payload.put("settings", Map.of("language", "JAVA", "infrastructure", "EKS"));
        String identifier = post(path, payload).statusCode(201).extract().path("identifier");
        payload.put(
            "version",
            get(path + "/" + identifier)
                .statusCode(200)
                .extract()
                .path("version")
        );
        payload.put("settings", null);
        put(path + "/" + identifier, payload).statusCode(400);
        payload.remove("settings");
        put(path + "/" + identifier, payload).statusCode(400);
        payload.put("settings", Map.of("language", "JAVA", "infrastructure", "EKS", "extra", true));
        put(path + "/" + identifier, payload).statusCode(400);
        payload.put("settings", Map.of("language", "JAVA", "infrastructure", "VM"));
        put(path + "/" + identifier, payload).statusCode(200);
    }

    @Test
    void acceptsAbsentAndNullOptionalFieldsLikeWorkspace() {
        String workspace = createWorkspace("MANAGER");
        String path = "/api/v1/workspaces/" + workspace + "/applications";

        Map<String, Object> absentOptional = request("Optional Fields Absent");
        absentOptional.remove("tags");
        post(path, absentOptional).statusCode(201);

        Map<String, Object> nullAuthorizerGroup = request("Null Authorizer Group");
        nullAuthorizerGroup.put("authorizerGroup", null);
        post(path, nullAuthorizerGroup).statusCode(201).body("authorizerGroup", nullValue());

        Map<String, Object> nullSettings = request("Null Settings");
        nullSettings.put("settings", null);
        post(path, nullSettings).statusCode(400);
        nullSettings.remove("settings");
        post(path, nullSettings).statusCode(400);

        Map<String, Object> nullTags = request("Null Tags");
        nullTags.put("tags", null);
        post(path, nullTags).statusCode(201).body("tags", org.hamcrest.Matchers.empty());
    }

    @Test
    void enforcesApplicationAuthorizationLevelsAndResourceVisibility() {
        String workspace = createWorkspace("MANAGER", "PARENT_A");
        String path = "/api/v1/workspaces/" + workspace + "/applications";
        Map<String, Object> teamA = request("Team A Application");
        teamA.put("authorizerGroup", "TEAM_A");
        String appA = post(path, teamA).statusCode(201).extract().path("identifier");
        Map<String, Object> teamB = request("Team B Application");
        teamB.put("authorizerGroup", "TEAM_B");
        String appB = post(path, teamB).statusCode(201).extract().path("identifier");
        Integer versionB = get(path + "/" + appB)
            .statusCode(200)
            .extract()
            .path("version");

        authorization.reset();
        authorization.allow(session ->
            session
                .groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_DEV_PARENT_A", "DEV", "DEV", "PARENT_A")
                .addAuthorizerGroup("GRP_APPLICATION_DEV_TEAM_A", "DEV", "DEV", "A-TEAM_A")
        );
        Integer version = get(path + "/" + appA)
            .statusCode(200)
            .extract()
            .path("version");
        authorization.verifyCalledWithPolicy("DEV");
        get(path + "/" + appB)
            .statusCode(404)
            .body("code", equalTo("APPLICATION-0001"));
        get(path)
            .statusCode(200)
            .body("content.identifier", hasItem(appA))
            .body("content.identifier", not(hasItem(appB)))
            .body("page.totalElements", equalTo(1));
        get(path + "/summary")
            .statusCode(200)
            .body("identifier", hasItem(appA))
            .body("identifier", not(hasItem(appB)));

        Map<String, Object> update = request("Team A Updated");
        update.put("authorizerGroup", "TEAM_A");
        update.put("version", version);
        put(path + "/" + appA, update).statusCode(200);
        authorization.verifyCalledWithPolicy("DEV");
        Map<String, Object> otherUpdate = request("Team B Changed Without Access");
        otherUpdate.put("authorizerGroup", "TEAM_B");
        otherUpdate.put("version", versionB);
        put(path + "/" + appB, otherUpdate)
            .statusCode(404)
            .body("code", equalTo("APPLICATION-0001"));

        authorization.forbidden();
        post(path + "/" + appA + "/inactivate", null).statusCode(403);
        authorization.verifyCalledWithPolicy("ADM");
        post(path, request("New Application")).statusCode(403);

        authorization.reset();
        authorization.allow(session ->
            session
                .groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_ADM_PARENT_A", "ADM", "ADM", "PARENT_A")
                .addAuthorizerGroup("GRP_APPLICATION_ADM_TEAM_A", "ADM", "ADM", "A-TEAM_A")
        );
        post(path + "/" + appA + "/inactivate", null).statusCode(204);
        authorization.verifyCalledWithPolicy("ADM");
        post(path + "/" + appA + "/restore", null).statusCode(200);
    }

    @Test
    void requiresVisibilityOfParentWorkspaceForCreate() {
        String workspace = createWorkspace("MANAGER", "PARENT_A");
        String path = "/api/v1/workspaces/" + workspace + "/applications";

        authorization.reset();
        authorization.allow(session ->
            session.userName("test-user").groups("USER").addAuthorizerGroup("GRP_WORKSPACE_ADM_OTHER", "ADM", "ADM", "OTHER")
        );
        post(path, request("Outside Workspace")).statusCode(404).body("code", equalTo("WORKSPACE-0001"));
        authorization.verifyCalledWithPolicy("ADM");

        authorization.reset();
        authorization.allow(session ->
            session.userName("test-user").groups("USER").addAuthorizerGroup("GRP_WORKSPACE_ADM_PARENT_A", "ADM", "ADM", "PARENT_A")
        );
        post(path, request("Inside Workspace")).statusCode(201);
    }

    @Test
    void shouldRejectApplicationWritesWhenParentWorkspaceIsInactiveOrQuarantined() {
        String workspaceIdentifier = createWorkspace("MANAGER");
        String path = "/api/v1/workspaces/" + workspaceIdentifier + "/applications";
        String applicationIdentifier = post(path, request("Parent Lifecycle App"))
            .statusCode(201)
            .extract()
            .path("identifier");
        post("/api/v1/workspaces/" + workspaceIdentifier + "/inactivate", Map.of()).statusCode(204);
        post(path, request("Inactive Parent App")).statusCode(404);
        post(path + "/" + applicationIdentifier + "/inactivate", Map.of()).statusCode(404);
        given()
            .port(port)
            .header("correlation-id", "application-api-it")
            .header("Authorization", "Bearer application-api-it")
            .when()
            .delete("/api/v1/workspaces/" + workspaceIdentifier)
            .then()
            .statusCode(204);
        post(path, request("Quarantined Parent App")).statusCode(404);
    }

    private String createWorkspace(String type) {
        return createWorkspace(type, null);
    }

    private String createWorkspace(String type, String authorizerGroup) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workspaceType", type);
        if (authorizerGroup != null) request.put("authorizerGroup", authorizerGroup);
        request.put("name", "Workspace " + UUID.randomUUID());
        request.put("description", "Workspace de teste para aplicação");
        request.put("requester", "requester");
        request.put("acronym", "APP");
        request.put("settings", Map.of());
        request.put("emailGroup", "workspace@portalmanager.com");
        request.put("approvers", List.of(Map.of("functional", "F1234", "email", "approver@portalmanager.com")));
        return post("/api/v1/workspaces", request).statusCode(201).extract().path("identifier");
    }

    private Map<String, Object> request(String name) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", name);
        value.put("alias", "portal");
        value.put("acronym", "APP");
        value.put("applicationScope", "BACKEND");
        value.put("settings", Map.of("language", "JAVA", "infrastructure", "VM"));
        value.put("tags", List.of("managed"));
        return value;
    }

    private ValidatableResponse get(String path) {
        return given()
            .port(port)
            .header("correlation-id", "application-api-it")
            .header("Authorization", "Bearer application-api-it")
            .accept(ContentType.JSON)
            .when()
            .get(path)
            .then();
    }

    private ValidatableResponse getByTag(String path, String tag) {
        return given()
            .port(port)
            .header("correlation-id", "application-api-it")
            .header("Authorization", "Bearer application-api-it")
            .accept(ContentType.JSON)
            .queryParam("tagName", tag)
            .when()
            .get(path)
            .then();
    }

    private ValidatableResponse post(String path, Object body) {
        RequestSpecification request = given()
            .port(port)
            .header("correlation-id", "application-api-it")
            .header("Authorization", "Bearer application-api-it")
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON);
        if (body != null) request.body(body);
        return request.when().post(path).then();
    }

    private ValidatableResponse put(String path, Object body) {
        return given()
            .port(port)
            .header("correlation-id", "application-api-it")
            .header("Authorization", "Bearer application-api-it")
            .contentType(ContentType.JSON)
            .body(body)
            .when()
            .put(path)
            .then();
    }

    private ValidatableResponse patch(String path) {
        return given()
            .port(port)
            .header("correlation-id", "application-api-it")
            .header("Authorization", "Bearer application-api-it")
            .accept(ContentType.JSON)
            .when()
            .patch(path)
            .then();
    }

    private ValidatableResponse delete(String path) {
        return given()
            .port(port)
            .header("correlation-id", "application-api-it")
            .header("Authorization", "Bearer application-api-it")
            .when()
            .delete(path)
            .then();
    }
}
