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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class WorkspaceTaggingIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorizationMock authorizationMock;

    @BeforeEach
    void authorizeAsOwner() {
        seedWorkspaceTypes();
        seedLifecycleTypes();
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void shouldNormalizeExposeManualTagsAndHideSystemTags() {
        Map<String, Object> request = validCreate("Workspace Tags", "TAG", "TEAM_A");
        request.put("tags", List.of("  Minha   Tag  ", "minha\tTag", "OUTRA TAG", "   "));

        String identifier = post(request)
                .statusCode(201)
                .body("tags", containsInAnyOrder("minha-tag", "outra-tag"))
                .body("tags", not(hasItem("workspace-tags")))
                .extract()
                .path("identifier");

        getList("Minha Tag")
                .statusCode(200)
                .body("identifier", hasItem(identifier));

        getList("Workspace Tags")
                .statusCode(200)
                .body("identifier", hasItem(identifier));

        getList("TEAM_A")
                .statusCode(200)
                .body("identifier", hasItem(identifier));
    }

    @Test
    void shouldRecalculateSystemTagsOnUpdateAndPreserveManualTags() {
        Map<String, Object> create = validCreate("Workspace Original", "ORG", "TEAM_A");
        create.put("tags", List.of("manual-tag"));

        var created = post(create).statusCode(201).extract();
        String identifier = created.path("identifier");
        Integer version = created.path("version");

        Map<String, Object> update = validUpdate(version, "Workspace Atualizado", "ATU", "TEAM_B");
        update.put("tags", List.of("manual-tag"));

        put(identifier, update)
                .statusCode(200)
                .body("tags", containsInAnyOrder("manual-tag"));

        getList("Workspace Original")
                .statusCode(200)
                .body("identifier", not(hasItem(identifier)));

        getList("ORG")
                .statusCode(200)
                .body("identifier", not(hasItem(identifier)));

        getList("Workspace Atualizado")
                .statusCode(200)
                .body("identifier", hasItem(identifier));

        getList("ATU")
                .statusCode(200)
                .body("identifier", hasItem(identifier));

        getList("manual-tag")
                .statusCode(200)
                .body("identifier", hasItem(identifier));
    }

    @Test
    void shouldRemoveTagsWhenWorkspaceIsPhysicallyDeleted() {
        Map<String, Object> create = validCreate("Workspace Delete Tags", "DEL", "TEAM_DELETE");
        create.put("tags", List.of("delete-tag"));

        String identifier = post(create)
                .statusCode(201)
                .extract()
                .path("identifier");

        post("/api/v1/workspaces/" + identifier + "/inactivate")
                .statusCode(204);

        delete("/api/v1/workspaces/" + identifier)
                .statusCode(204);

        getList("delete-tag")
                .statusCode(200)
                .body("identifier", not(hasItem(identifier)));
    }

    private Map<String, Object> validCreate(String name, String acronym, String authorizerGroup) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workspaceType", "ADMIN");
        request.put("name", name);
        request.put("description", "Descrição válida para " + name);
        request.put("requester", "requester");
        request.put("acronym", acronym);
        request.put("authorizerGroup", authorizerGroup);
        request.put("settings", "{\"feature\":true}");
        request.put("emailGroup", "workspace@portalmanager.com");
        request.put("approvers", List.of(Map.of(
                "functional", "F1000",
                "email", "approver@portalmanager.com"
        )));
        return request;
    }

    private Map<String, Object> validUpdate(
            Integer version,
            String name,
            String acronym,
            String authorizerGroup
    ) {
        Map<String, Object> request = validCreate(name, acronym, authorizerGroup);
        request.put("version", version);
        return request;
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

    private io.restassured.response.ValidatableResponse post(String path) {
        return authorized()
                .when()
                .post(path)
                .then();
    }

    private io.restassured.response.ValidatableResponse delete(String path) {
        return authorized()
                .when()
                .delete(path)
                .then();
    }

    private io.restassured.response.ValidatableResponse getList(String tagName) {
        return authorized()
                .queryParam("tagName", tagName)
                .when()
                .get("/api/v1/workspaces")
                .then();
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
                .port(port)
                .header("X-Correlation-Id", "tagging-it")
                .header("Authorization", "Bearer tagging-it")
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
        jdbcTemplate.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('PENDING_DELETION', 'Pending deletion', 'Pending physical deletion', 3, true, '{}')");
    }

}
