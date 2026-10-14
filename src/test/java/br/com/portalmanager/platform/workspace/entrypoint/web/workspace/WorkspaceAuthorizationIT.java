package br.com.portalmanager.platform.workspace.entrypoint.web.workspace;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

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
        SchemaDefaultFixture.seed(jdbcTemplate);
        seedWorkspaceTypes();
        seedLifecycleTypes();
        allowOwner();
    }

    @Test
    void shouldRestrictWorkspaceCreationByTypeAndAuthorizerGroup() {
        authorizationMock.reset();
        authorizationMock.allow(session -> session.userName("test-user").groups("USER"));
        Map<String, Object> request = validCreate("Restricted Creation", "TEAM_A");
        request.put("workspaceType", "MANAGER");
        post(request).statusCode(403).body("code", equalTo("WORKSPACE-0120"));
        request.put("workspaceType", "ADMIN");
        request.remove("authorizerGroup");
        post(request).statusCode(403).body("code", equalTo("WORKSPACE-0120"));
        request.put("authorizerGroup", "TEAM_A");
        post(request).statusCode(403);
        org.assertj.core.api.Assertions.assertThat(
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM workspaces", Long.class)
        ).isZero();
        request.put("workspaceType", "MANAGER");
        request.remove("authorizerGroup");
        post(request).statusCode(201).body("workspaceType", equalTo("MANAGER")).body("authorizerGroup", nullValue());
        request.put("name", "Blank Authorizer Creation");
        request.put("authorizerGroup", "   ");
        post(request).statusCode(201).body("authorizerGroup", nullValue());
        allowOwner();
        request.put("name", "Owner Admin Creation");
        request.put("workspaceType", "ADMIN");
        request.put("authorizerGroup", "TEAM_A");
        post(request)
            .statusCode(201)
            .body("workspaceType", equalTo("ADMIN"))
            .body("authorizerGroup", equalTo("TEAM_A"));
        request.put("name", "Owner Manager Creation");
        request.put("workspaceType", "MANAGER");
        post(request)
            .statusCode(201)
            .body("workspaceType", equalTo("MANAGER"))
            .body("authorizerGroup", equalTo("TEAM_A"));
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
    void shouldRejectMissingCorrelationIdEvenWithBearerToken() {
        given()
            .port(port)
            .header("Authorization", "Bearer authorization-it")
            .accept(ContentType.JSON)
            .when()
            .get("/api/v1/workspaces")
            .then()
            .statusCode(401)
            .body("code", equalTo("AUTH-401-001"));
    }

    @Test
    void shouldRejectMissingBearerTokenEvenWithCorrelationId() {
        given()
            .port(port)
            .header("correlation-id", "authorization-it")
            .accept(ContentType.JSON)
            .when()
            .get("/api/v1/workspaces")
            .then()
            .statusCode(401)
            .body("code", equalTo("AUTH-401-002"));
    }

    @Test
    void shouldApplyOpenDevAndAdmPolicies() {
        String identifier = post(validCreate("Workspace Policy", "POLICY"))
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
        Map<String, Object> update = validCreate("Workspace Policy", "POLICY");
        update.put("version", version);
        update.put("description", "Descrição atualizada para policy");

        put(identifier, update).statusCode(200);
        authorizationMock.verifyCalledWithPolicy("ADM");
    }

    @Test
    void shouldApplyResourceVisibilityForSingleAndCollectionReads() {
        String teamA = post(validCreate("Workspace Team A", "TEAM_A")).statusCode(201).extract().path("identifier");

        String teamB = post(validCreate("Workspace Team B", "TEAM_B")).statusCode(201).extract().path("identifier");

        authorizationMock.reset();
        authorizationMock.allow(session ->
            session.userName("test-user").groups("USER").addAuthorizerGroup("GRP_WORKSPACE_DEV_TEAM_A", "DEV", "DEV", "TEAM_A")
        );

        get("/api/v1/workspaces/" + teamA)
            .statusCode(200)
            .body("identifier", equalTo(teamA));

        get("/api/v1/workspaces")
            .statusCode(200)
            .body("content.identifier", containsInAnyOrder(teamA))
            .body("page.totalElements", equalTo(1));

        get("/api/v1/workspaces/" + teamB)
            .statusCode(404)
            .body("code", equalTo("WORKSPACE-0001"));

        allowOwner();
        get("/api/v1/workspaces/" + teamB)
            .statusCode(200)
            .body("identifier", equalTo(teamB));
    }

    @Test
    void shouldRejectWritesOutsideTheAuthorizedWorkspaceGroup() {
        String teamB = post(validCreate("Workspace Protected", "TEAM_B")).statusCode(201).extract().path("identifier");
        Integer version = get("/api/v1/workspaces/" + teamB)
            .statusCode(200)
            .extract()
            .path("version");
        Map<String, Object> update = validCreate("Workspace Protected", "TEAM_B");
        update.put("version", version);
        update.put("description", "Alteração não autorizada");
        allowTeamAAdministrator();
        put(teamB, update).statusCode(404).body("code", equalTo("WORKSPACE-0001"));
        authorized()
            .when()
            .post("/api/v1/workspaces/" + teamB + "/inactivate")
            .then()
            .statusCode(404);
        allowOwner();
        get("/api/v1/workspaces/" + teamB)
            .statusCode(200)
            .body("version", equalTo(version));
        authorized()
            .when()
            .post("/api/v1/workspaces/" + teamB + "/inactivate")
            .then()
            .statusCode(204);
        allowTeamAAdministrator();
        authorized()
            .when()
            .post("/api/v1/workspaces/" + teamB + "/restore")
            .then()
            .statusCode(404);
        authorized()
            .when()
            .delete("/api/v1/workspaces/" + teamB)
            .then()
            .statusCode(404);
        allowOwner();
        authorized()
            .when()
            .post("/api/v1/workspaces/" + teamB + "/restore")
            .then()
            .statusCode(200);
    }

    @Test
    void shouldAllowAuthorizedWritesAndKeepNameUniquenessAcrossGroups() {
        String teamA = post(validCreate("Workspace Writable A", "TEAM_A")).statusCode(201).extract().path("identifier");
        post(validCreate("Workspace Writable B", "TEAM_B")).statusCode(201);
        Integer version = get("/api/v1/workspaces/" + teamA)
            .statusCode(200)
            .extract()
            .path("version");
        allowTeamAAdministrator();
        Map<String, Object> update = validCreate("Workspace Writable B", "TEAM_A");
        update.put("version", version);
        put(teamA, update).statusCode(400);
        update.put("name", "Workspace Writable A");
        put(teamA, update).statusCode(200);
        authorized()
            .when()
            .post("/api/v1/workspaces/" + teamA + "/inactivate")
            .then()
            .statusCode(204);
        authorized()
            .when()
            .post("/api/v1/workspaces/" + teamA + "/restore")
            .then()
            .statusCode(200);
        authorized()
            .when()
            .post("/api/v1/workspaces/" + teamA + "/inactivate")
            .then()
            .statusCode(204);
        authorized()
            .when()
            .delete("/api/v1/workspaces/" + teamA)
            .then()
            .statusCode(204);
    }

    @Test
    void shouldRequireOwnerOnlyWhenChangingTypeOrAuthorizerGroup() {
        String identifier = post(validCreate("Workspace Owner Fields", "TEAM_A"))
            .statusCode(201)
            .extract()
            .path("identifier");
        Integer version = get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");
        allowTeamAAdministrator();
        Map<String, Object> update = validCreate("Workspace Owner Fields", "TEAM_A");
        update.put("version", version);
        update.put("workspaceType", "MANAGER");
        put(identifier, update).statusCode(403);
        update.put("workspaceType", "ADMIN");
        update.put("authorizerGroup", "TEAM_B");
        put(identifier, update).statusCode(403);
        update.put("authorizerGroup", null);
        put(identifier, update).statusCode(403);
        update.put("authorizerGroup", " team_a ");
        update.put("description", "Descrição alterada pelo administrador");
        Integer updatedVersion = put(identifier, update).statusCode(200).extract().path("version");
        allowOwner();
        update.put("version", updatedVersion);
        update.put("workspaceType", "MANAGER");
        update.put("authorizerGroup", "TEAM_B");
        updatedVersion = put(identifier, update)
            .statusCode(200)
            .body("workspaceType", equalTo("MANAGER"))
            .body("authorizerGroup", equalTo("TEAM_B"))
            .extract()
            .path("version");
        update.put("version", updatedVersion);
        update.put("authorizerGroup", null);
        put(identifier, update).statusCode(200).body("authorizerGroup", nullValue());
    }

    @Test
    void shouldApplyVisibilityToPagedContentCountsAndTagFilters() {
        for (int index = 0; index < 3; index++) {
            Map<String, Object> request = validCreate("Workspace Visible " + index, "TEAM_A");
            request.put("tags", List.of("common-tag"));
            post(request).statusCode(201);
        }
        Map<String, Object> hidden = validCreate("Workspace Hidden", "TEAM_B");
        hidden.put("tags", List.of("common-tag"));
        post(hidden).statusCode(201);
        allowTeamAAdministrator();
        get("/api/v1/workspaces?page=0&size=1&tagName=common-tag")
            .statusCode(200)
            .body("content", hasSize(1))
            .body("page.totalElements", equalTo(3))
            .body("page.totalPages", equalTo(3))
            .body("content.authorizerGroup", contains("TEAM_A"));
        get("/api/v1/workspaces?page=2&size=1&tagName=common-tag")
            .statusCode(200)
            .body("content", hasSize(1))
            .body("page.totalElements", equalTo(3));
        get("/api/v1/workspaces?page=3&size=1&tagName=common-tag")
            .statusCode(200)
            .body("content", hasSize(0))
            .body("page.totalElements", equalTo(3));
    }

    private void allowTeamAAdministrator() {
        authorizationMock.reset();
        authorizationMock.allow(session ->
            session.userName("test-user").groups("USER").addAuthorizerGroup("GRP_WORKSPACE_ADM_TEAM_A", "ADM", "ADM", "TEAM_A")
        );
    }

    private void allowOwner() {
        authorizationMock.reset();
        authorizationMock.allow(session -> session.userName("test-user").groups("PM5_OWNER"));
    }

    private Map<String, Object> validCreate(String name, String authorizerGroup) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workspaceType", "ADMIN");
        request.put("name", name);
        request.put("description", "Descrição válida para " + name);
        request.put("requester", "requester");
        request.put("acronym", "AUTH");
        request.put("authorizerGroup", authorizerGroup);
        request.put("settings", Map.of("feature", true));
        request.put("emailGroup", "workspace@portalmanager.com");
        request.put("approvers", List.of(Map.of("functional", "F1000", "email", "approver@portalmanager.com")));
        request.put("tags", List.of());
        return request;
    }

    private io.restassured.response.ValidatableResponse get(String path) {
        return authorized().when().get(path).then();
    }

    private io.restassured.response.ValidatableResponse post(Map<String, Object> body) {
        return authorized().contentType(ContentType.JSON).body(body).when().post("/api/v1/workspaces").then();
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
            .header("correlation-id", "authorization-it")
            .header("Authorization", "Bearer authorization-it")
            .accept(ContentType.JSON);
    }

    private void seedWorkspaceTypes() {
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 'Management workspace', 2, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('CATALOG', 'Catalog', 'Catalog workspace', 3, true, '{}')"
        );
    }

    private void seedLifecycleTypes() {
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}')"
        );
    }
}
