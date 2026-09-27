package br.com.portalmanager.platform.workspace.entrypoint.web.application;

import br.com.portalmanager.platform.library.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class ApplicationApiIT {
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthorizationMock authorization;

    @BeforeEach
    void prepare() {
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
                .body("settings", equalTo("{}"))
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
        create.put("tags", List.of("  Minha   Tag  ", "minha\tTag", "OUTRA TAG"));
        var created = post(path, create).statusCode(201)
                .body("tags", containsInAnyOrder("minha-tag", "outra-tag"))
                .body("authorizerGroup", equalTo("A-TEAM_A"))
                .extract();
        String app = created.path("identifier");
        Integer version = created.path("version");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tags WHERE owner_type='APPLICATION' AND owner_id=? AND origin_type='MANUAL'", Integer.class, app))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tags WHERE owner_type='APPLICATION' AND owner_id=? AND origin_type='SYSTEM'", Integer.class, app))
                .isGreaterThan(0);
        getByTag(path, "Minha Tag").statusCode(200).body("identifier", hasItem(app));
        getByTag(path, "A-TEAM_A").statusCode(200).body("identifier", hasItem(app));
        getByTag(path, "Application Original").statusCode(200).body("identifier", hasItem(app));

        Map<String, Object> update = request("Application Updated");
        update.put("version", version);
        update.put("authorizerGroup", "TEAM_B");
        update.put("tags", List.of("Minha Tag"));
        put(path + "/" + app, update).statusCode(200).body("tags", contains("minha-tag"));
        getByTag(path, "Application Original").statusCode(200).body("identifier", not(hasItem(app)));
        getByTag(path, "A-TEAM_A").statusCode(200).body("identifier", not(hasItem(app)));
        getByTag(path, "A-TEAM_B").statusCode(200).body("identifier", hasItem(app));
        getByTag(path, "Outra Tag").statusCode(200).body("identifier", not(hasItem(app)));

        post(path + "/" + app + "/inactivate", null).statusCode(204);
        post(path + "/" + app + "/restore", null).statusCode(200).body("tags", contains("minha-tag"));
        getByTag(path, "Minha Tag").statusCode(200).body("identifier", hasItem(app));
    }

    private String createWorkspace(String type) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workspaceType", type);
        request.put("name", "Workspace " + UUID.randomUUID());
        request.put("description", "Workspace de teste para aplicação");
        request.put("requester", "requester");
        request.put("acronym", "APP");
        request.put("settings", "{}");
        request.put("emailGroup", "workspace@portalmanager.com");
        request.put("approvers", List.of(Map.of("functional", "F1234", "email", "approver@portalmanager.com")));
        return post("/api/v1/workspaces", request).statusCode(201).extract().path("identifier");
    }

    private Map<String, Object> request(String name) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", name);
        value.put("alias", "portal");
        value.put("acronym", "APP");
        value.put("applicationScope", " backend ");
        value.put("tags", List.of("managed"));
        return value;
    }

    private ValidatableResponse get(String path) {
        return given().port(port).header("X-Correlation-Id", "application-api-it")
                .header("Authorization", "Bearer application-api-it").accept(ContentType.JSON)
                .when().get(path).then();
    }

    private ValidatableResponse getByTag(String path, String tag) {
        return given().port(port).header("X-Correlation-Id", "application-api-it")
                .header("Authorization", "Bearer application-api-it").accept(ContentType.JSON)
                .queryParam("tagName", tag).when().get(path).then();
    }

    private ValidatableResponse post(String path, Object body) {
        var request = given().port(port).header("X-Correlation-Id", "application-api-it")
                .header("Authorization", "Bearer application-api-it").contentType(ContentType.JSON).accept(ContentType.JSON);
        if (body != null) request.body(body);
        return request.when().post(path).then();
    }

    private ValidatableResponse put(String path, Object body) {
        return given().port(port).header("X-Correlation-Id", "application-api-it")
                .header("Authorization", "Bearer application-api-it").contentType(ContentType.JSON)
                .body(body).when().put(path).then();
    }

    private ValidatableResponse delete(String path) {
        return given().port(port).header("X-Correlation-Id", "application-api-it")
                .header("Authorization", "Bearer application-api-it").when().delete(path).then();
    }
}
