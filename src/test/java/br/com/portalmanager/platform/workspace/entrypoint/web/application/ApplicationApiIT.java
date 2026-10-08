package br.com.portalmanager.platform.workspace.entrypoint.web.application;

import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.entrypoint.web.application.request.CreateApplicationRequest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class ApplicationApiIT {
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthorizationMock authorization;
    @Autowired private ObjectMapper json;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update("INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 'Management workspace', 2, true, '{}')");
        jdbc.update("INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}')");
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('ACTIVE', 'Active', 'Active lifecycle', 1, true, '{}')");
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('INACTIVE', 'Inactive', 'Inactive lifecycle', 2, true, '{}')");
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle', 3, true, '{}')");
        jdbc.update("INSERT IGNORE INTO type_application_scopes (code, label, description, sort_order, is_active, settings) VALUES ('BACKEND', 'Backend', 'Backend application', 1, true, '{}')");
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void createsUpdatesAndTransitionsThroughLifecycle() {
        String workspace = createWorkspace("MANAGER");
        String path = "/api/v1/workspaces/" + workspace + "/applications";
        String app = post(path, request("Portal App"))
                .statusCode(201)
                .body("applicationScope", equalTo("BACKEND"))
                .body("authorizerGroup", nullValue())
                .body("settings", anEmptyMap())
                .body("lifecycle", equalTo("ACTIVE"))
                .extract().path("identifier");

        Integer version = get(path + "/" + app).statusCode(200).extract().path("version");
        Map<String, Object> update = request("Portal App Updated");
        update.put("version", version);
        put(path + "/" + app, update).statusCode(200).body("version", greaterThan(version));
        put(path + "/" + app, update).statusCode(409);

        get(path + "/summary").statusCode(200).body("identifier", hasItem(app));
        post(path + "/" + app + "/inactivate", null).statusCode(204);
        get(path + "/" + app).statusCode(404);
        get(path + "?active=false").statusCode(200).body("identifier", hasItem(app));
        post(path + "/" + app + "/restore", null).statusCode(200).body("lifecycle", equalTo("ACTIVE"));
        delete(path + "/" + app).statusCode(400);
        post(path + "/" + app + "/inactivate", null).statusCode(204);
        delete(path + "/" + app).statusCode(204);
        get(path + "?active=false").statusCode(200).body("identifier", not(hasItem(app)));
    }

    @Test
    void readsApplicationRequestWithoutDefaultFlag() {
        CreateApplicationRequest parsed = json.readValue(json.writeValueAsString(request("Request Mapping")),
                CreateApplicationRequest.class);
        assertThat(parsed.toInput().name()).isEqualTo("Request Mapping");
    }

    @Test
    void rejectsNonManagerWorkspaceAndInvalidScope() {
        String admin = createWorkspace("ADMIN");
        post("/api/v1/workspaces/" + admin + "/applications", request("Admin App"))
                .statusCode(400).body("details.field", hasItem("workspaceIdentifier"));

        String manager = createWorkspace("MANAGER");
        Map<String, Object> invalid = request("Invalid Scope");
        invalid.put("applicationScope", "UNKNOWN");
        post("/api/v1/workspaces/" + manager + "/applications", invalid)
                .statusCode(400).body("details.field", hasItem("applicationScope"));
    }

    @Test
    void separatesManualAndSystemTagsAndReconcilesThemOnUpdateAndRestore() {
        String workspace = createWorkspace("MANAGER");
        String path = "/api/v1/workspaces/" + workspace + "/applications";
        Map<String, Object> create = request("Application Original");
        create.put("authorizerGroup", "TEAM_A");
        create.put("tags", List.of("minha-tag", "outra-tag"));
        var created = post(path, create).statusCode(201)
                .body("tags", containsInAnyOrder("minha-tag", "outra-tag"))
                .body("authorizerGroup", equalTo("A-TEAM_A"))
                .extract();
        String app = created.path("identifier");
        Integer version = created.path("version");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM application_tag t JOIN applications a ON a.id=t.application_id WHERE a.identifier=? AND t.origin_type='MANUAL'", Integer.class, app))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM application_tag t JOIN applications a ON a.id=t.application_id WHERE a.identifier=? AND t.origin_type='SYSTEM'", Integer.class, app))
                .isGreaterThan(0);
        getByTag(path, "Minha Tag").statusCode(200).body("identifier", hasItem(app));
        getByTag(path, "A-TEAM_A").statusCode(200).body("identifier", hasItem(app));
        getByTag(path, "Application Original").statusCode(200).body("identifier", hasItem(app));

        Map<String, Object> update = request("Application Updated");
        update.put("version", version);
        update.put("authorizerGroup", "TEAM_B");
        update.put("tags", List.of("minha-tag"));
        put(path + "/" + app, update).statusCode(200).body("tags", contains("minha-tag"));
        getByTag(path, "Application Original").statusCode(200).body("identifier", not(hasItem(app)));
        getByTag(path, "A-TEAM_A").statusCode(200).body("identifier", not(hasItem(app)));
        getByTag(path, "A-TEAM_B").statusCode(200).body("identifier", hasItem(app));
        getByTag(path, "Outra Tag").statusCode(200).body("identifier", not(hasItem(app)));

        post(path + "/" + app + "/inactivate", null).statusCode(204);
        post(path + "/" + app + "/restore", null).statusCode(200).body("tags", contains("minha-tag"));
        getByTag(path, "Minha Tag").statusCode(200).body("identifier", hasItem(app));
    }

    @Test
    void rejectsInvalidApplicationResourceStructure() {
        String workspace = createWorkspace("MANAGER");
        String path = "/api/v1/workspaces/" + workspace + "/applications";

        Map<String, Object> duplicateTags = request("Duplicate Tags");
        duplicateTags.put("tags", List.of("managed", "managed"));
        post(path, duplicateTags).statusCode(400);

        Map<String, Object> invalidTag = request("Invalid Tag");
        invalidTag.put("tags", List.of("Managed"));
        post(path, invalidTag).statusCode(400);

        Map<String, Object> invalidSettings = request("Invalid Settings");
        invalidSettings.put("settings", "not-json-object");
        post(path, invalidSettings).statusCode(400);

        Map<String, Object> missingRequired = request("Missing Alias");
        missingRequired.remove("alias");
        post(path, missingRequired).statusCode(400);
    }

    @Test
    void distinguishesAbsentOptionalFieldsFromExplicitNull() {
        String workspace = createWorkspace("MANAGER");
        String path = "/api/v1/workspaces/" + workspace + "/applications";

        Map<String, Object> absentOptional = request("Optional Fields Absent");
        absentOptional.remove("tags");
        post(path, absentOptional).statusCode(201);

        Map<String, Object> nullAuthorizerGroup = request("Null Authorizer Group");
        nullAuthorizerGroup.put("authorizerGroup", null);
        post(path, nullAuthorizerGroup)
                .statusCode(400)
                .body("details.field", hasItem("authorizerGroup"));

        Map<String, Object> nullSettings = request("Null Settings");
        nullSettings.put("settings", null);
        post(path, nullSettings)
                .statusCode(400)
                .body("details.field", hasItem("settings"));

        Map<String, Object> nullTags = request("Null Tags");
        nullTags.put("tags", null);
        post(path, nullTags)
                .statusCode(400)
                .body("details.field", hasItem("tags"));
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
        Integer versionB = get(path + "/" + appB).statusCode(200).extract().path("version");

        authorization.reset();
        authorization.allow(session -> session.groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_DEV_PARENT_A", "DEV", "DEV", "PARENT_A")
                .addAuthorizerGroup("GRP_APPLICATION_DEV_TEAM_A", "DEV", "DEV", "A-TEAM_A"));
        Integer version = get(path + "/" + appA).statusCode(200).extract().path("version");
        authorization.verifyCalledWithPolicy("DEV");
        get(path + "/" + appB).statusCode(404).body("code", equalTo("APPLICATION-0001"));
        get(path).statusCode(200).body("identifier", hasItem(appA)).body("identifier", not(hasItem(appB)));
        get(path + "/summary").statusCode(200).body("identifier", hasItem(appA)).body("identifier", not(hasItem(appB)));

        Map<String, Object> update = request("Team A Updated");
        update.put("authorizerGroup", "TEAM_A");
        update.put("version", version);
        put(path + "/" + appA, update).statusCode(200);
        authorization.verifyCalledWithPolicy("DEV");
        Map<String, Object> otherUpdate = request("Team B Changed Without Access");
        otherUpdate.put("authorizerGroup", "TEAM_B");
        otherUpdate.put("version", versionB);
        put(path + "/" + appB, otherUpdate).statusCode(404).body("code", equalTo("APPLICATION-0001"));

        authorization.forbidden();
        post(path + "/" + appA + "/inactivate", null).statusCode(403);
        authorization.verifyCalledWithPolicy("ADM");
        post(path, request("New Application")).statusCode(403);

        authorization.reset();
        authorization.allow(session -> session.groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_ADM_PARENT_A", "ADM", "ADM", "PARENT_A")
                .addAuthorizerGroup("GRP_APPLICATION_ADM_TEAM_A", "ADM", "ADM", "A-TEAM_A"));
        post(path + "/" + appA + "/inactivate", null).statusCode(204);
        authorization.verifyCalledWithPolicy("ADM");
        post(path + "/" + appA + "/restore", null).statusCode(200);
    }

    @Test
    void requiresVisibilityOfParentWorkspaceForCreate() {
        String workspace = createWorkspace("MANAGER", "PARENT_A");
        String path = "/api/v1/workspaces/" + workspace + "/applications";

        authorization.reset();
        authorization.allow(session -> session.groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_ADM_OTHER", "ADM", "ADM", "OTHER"));
        post(path, request("Outside Workspace")).statusCode(404).body("code", equalTo("WORKSPACE-0001"));
        authorization.verifyCalledWithPolicy("ADM");

        authorization.reset();
        authorization.allow(session -> session.groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_ADM_PARENT_A", "ADM", "ADM", "PARENT_A"));
        post(path, request("Inside Workspace")).statusCode(201);
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
        value.put("tags", List.of("managed"));
        return value;
    }

    private ValidatableResponse get(String path) {
        return given().port(port).header("correlationId", "application-api-it")
                .header("Authorization", "Bearer application-api-it").accept(ContentType.JSON)
                .when().get(path).then();
    }

    private ValidatableResponse getByTag(String path, String tag) {
        return given().port(port).header("correlationId", "application-api-it")
                .header("Authorization", "Bearer application-api-it").accept(ContentType.JSON)
                .queryParam("tagName", tag).when().get(path).then();
    }

    private ValidatableResponse post(String path, Object body) {
        var request = given().port(port).header("correlationId", "application-api-it")
                .header("Authorization", "Bearer application-api-it").contentType(ContentType.JSON).accept(ContentType.JSON);
        if (body != null) request.body(body);
        return request.when().post(path).then();
    }

    private ValidatableResponse put(String path, Object body) {
        return given().port(port).header("correlationId", "application-api-it")
                .header("Authorization", "Bearer application-api-it").contentType(ContentType.JSON)
                .body(body).when().put(path).then();
    }

    private ValidatableResponse delete(String path) {
        return given().port(port).header("correlationId", "application-api-it")
                .header("Authorization", "Bearer application-api-it").when().delete(path).then();
    }
}
