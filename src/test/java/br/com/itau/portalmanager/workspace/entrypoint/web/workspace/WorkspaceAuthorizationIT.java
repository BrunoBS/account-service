package br.com.itau.portalmanager.workspace.entrypoint.web.workspace;

import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
import br.com.portalmanager.platform.testing.authorization.AuthorizationMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class WorkspaceAuthorizationIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorizationMock authorizationMock;

    @BeforeEach
    void resetAsOwner() {
        seedWorkspaceTypes();
        seedLifecycleTypes();
        allowOwner();
    }

    @Test
    void shouldRequireAuthenticationEvenForOpenPolicy() {
        given()
                .port(port)
                .accept(ContentType.JSON)
                .when()
                .get("/api/v1/workspaces")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTH-401-001"));
    }

    @Test
    void shouldApplyOpenDevAndAdmPolicies() {
        String identifier = post(validCreate("Workspace Policy", "TEAM_POLICY"))
                .statusCode(201)
                .extract()
                .path("identifier");
        authorizationMock.verifyCalledWithPolicy("OPEN");

        allowOwner();
        Integer version = get("/api/v1/workspaces/" + identifier)
                .statusCode(200)
                .extract()
                .path("version");
        authorizationMock.verifyCalledWithPolicy("DEV");

        allowOwner();
        Map<String, Object> update = validCreate("Workspace Policy", "TEAM_POLICY");
        update.put("version", version);
        update.put("description", "Descrição atualizada para policy");

        put(identifier, update).statusCode(200);
        authorizationMock.verifyCalledWithPolicy("ADM");
    }

    @Test
    void shouldApplyResourceVisibilityForSingleAndCollectionReads() {
        String teamA = post(validCreate("Workspace Team A", "TEAM_A"))
                .statusCode(201)
                .extract()
                .path("identifier");

        String teamB = post(validCreate("Workspace Team B", "TEAM_B"))
                .statusCode(201)
                .extract()
                .path("identifier");

        authorizationMock.reset();
        authorizationMock.allow(session -> session
                .groups("USER")
                .addAuthorizerGroup("GRP_WORKSPACE_DEV_TEAM_A", "DEV", "DEV", "TEAM_A"));

        get("/api/v1/workspaces/" + teamA)
                .statusCode(200)
                .body("identifier", equalTo(teamA));

        get("/api/v1/workspaces")
                .statusCode(200)
                .body("identifier", containsInAnyOrder(teamA));

        get("/api/v1/workspaces/" + teamB)
                .statusCode(403)
                .body("code", equalTo("AUTH-403-004"));

        allowOwner();
        get("/api/v1/workspaces/" + teamB)
                .statusCode(200)
                .body("identifier", equalTo(teamB));
    }

    private void allowOwner() {
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("PM5_OWNER"));
    }

    private Map<String, Object> validCreate(String name, String authorizerGroup) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workspaceType", "ADMIN");
        request.put("name", name);
        request.put("description", "Descrição válida para " + name);
        request.put("requester", "requester");
        request.put("acronym", "AUTH");
        request.put("authorizerGroup", authorizerGroup);
        request.put("settings", "{\"feature\":true}");
        request.put("emailGroup", "workspace@portalmanager.com");
        request.put("approvers", List.of(Map.of(
                "functional", "F1000",
                "email", "approver@portalmanager.com"
        )));
        request.put("tags", List.of());
        return request;
    }

    private io.restassured.response.ValidatableResponse get(String path) {
        return authorized().when().get(path).then();
    }

    private io.restassured.response.ValidatableResponse post(Map<String, Object> body) {
        return authorized()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/workspaces")
                .then();
    }

    private io.restassured.response.ValidatableResponse put(String identifier, Map<String, Object> body) {
        return authorized()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/api/v1/workspaces/" + identifier)
                .then();
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
                .port(port)
                .header("X-Correlation-Id", "authorization-it")
                .header("Authorization", "Bearer authorization-it")
                .accept(ContentType.JSON);
    }
    private void seedWorkspaceTypes() {
        jdbcTemplate.update("INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}')");
        jdbcTemplate.update("INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 'Management workspace', 2, true, '{}')");
        jdbcTemplate.update("INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('CATALOG', 'Catalog', 'Catalog workspace', 3, true, '{}')");
    }

    private void seedLifecycleTypes() {
        jdbcTemplate.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')");
        jdbcTemplate.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}')");
        jdbcTemplate.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}')");
    }

}
