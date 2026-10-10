package br.com.portalmanager.platform.workspace.entrypoint.web.workspace;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
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
class WorkspaceApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AuthorizationMock authorizationMock;

    @BeforeEach
    void authorizeAsOwner() {
        SchemaDefaultFixture.seed(jdbcTemplate);
        seedWorkspaceTypes();
        seedLifecycleTypes();
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void shouldCreateNormalizeAndReadActiveWorkspace() {
        Map<String, Object> request = validCreate("  Workspace G3  ", " admin ");
        request.put("description", "  Descrição válida do workspace G3  ");
        request.put("requester", "  requester  ");
        request.put("acronym", " G3 ");
        request.put("authorizerGroup", "  GRP_WORKSPACE  ");
        request.put("emailGroup", "  workspace@portalmanager.com  ");
        request.put("approvers", List.of(Map.of("functional", "  F1234  ", "email", "  approver@portalmanager.com  ")));

        String identifier = post(request)
            .statusCode(201)
            .body("id", nullValue())
            .body("identifier", not(nullValue()))
            .body("version", equalTo(0))
            .body("workspaceType", equalTo("ADMIN"))
            .body("name", equalTo("Workspace G3"))
            .body("description", equalTo("Descrição válida do workspace G3"))
            .body("requester", equalTo("requester"))
            .body("acronym", equalTo("G3"))
            .body("authorizerGroup", equalTo("GRP_WORKSPACE"))
            .body("emailGroup", equalTo("workspace@portalmanager.com"))
            .body("lifecycle", equalTo("ACTIVE"))
            .body("onboarding", equalTo(false))
            .body("approvers", hasSize(1))
            .body("approvers[0].functional", equalTo("F1234"))
            .extract()
            .path("identifier");

        get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .body("id", nullValue())
            .body("identifier", equalTo(identifier))
            .body("name", equalTo("Workspace G3"))
            .body("lifecycle", equalTo("ACTIVE"));
    }

    @Test
    void shouldListByLifecycleAndNormalizedTypeFilter() {
        String adminIdentifier = create("Workspace Admin", "ADMIN");
        String managerIdentifier = create("Workspace Manager", "MANAGER");

        post("/api/v1/workspaces/" + managerIdentifier + "/inactivate").statusCode(204);

        given()
            .port(port)
            .header("correlation-id", "workspace-api-it")
            .header("Authorization", "Bearer workspace-api-it")
            .accept(ContentType.JSON)
            .queryParam("active", true)
            .queryParam("typeName", " admin ")
            .when()
            .get("/api/v1/workspaces")
            .then()
            .statusCode(200)
            .body("identifier", hasItem(adminIdentifier))
            .body("identifier", not(hasItem(managerIdentifier)))
            .body("workspaceType", containsInAnyOrder("ADMIN"));

        get("/api/v1/workspaces?active=false")
            .statusCode(200)
            .body("identifier", containsInAnyOrder(managerIdentifier))
            .body("lifecycle", containsInAnyOrder("INACTIVE"));
    }

    @Test
    void shouldUpdateWorkspaceAndReplaceApprovers() {
        String identifier = create("Workspace Atualizável", "ADMIN");
        Integer version = get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");

        Map<String, Object> update = validUpdate(version, "Workspace Atualizável", "MANAGER");
        update.put("description", "Descrição atualizada com sucesso");
        update.put(
            "approvers",
            List.of(
                Map.of("functional", "F2000", "email", "new1@portalmanager.com"),
                Map.of("functional", "F3000", "email", "new2@portalmanager.com")
            )
        );

        put("/api/v1/workspaces/" + identifier, update)
            .statusCode(200)
            .body("id", nullValue())
            .body("identifier", equalTo(identifier))
            .body("version", greaterThan(version))
            .body("name", equalTo("Workspace Atualizável"))
            .body("workspaceType", equalTo("MANAGER"))
            .body("description", equalTo("Descrição atualizada com sucesso"))
            .body("approvers", hasSize(2))
            .body("approvers.functional", containsInAnyOrder("F2000", "F3000"));
    }

    @Test
    void shouldRejectStaleUpdateWithConflict() {
        String identifier = create("Workspace Concorrente", "ADMIN");
        Integer version = get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");

        Map<String, Object> first = validUpdate(version, "Workspace Concorrente", "ADMIN");
        first.put("description", "Descrição da primeira atualização");
        put("/api/v1/workspaces/" + identifier, first).statusCode(200);

        Map<String, Object> stale = validUpdate(version, "Workspace Concorrente", "ADMIN");
        stale.put("description", "Descrição da atualização obsoleta");

        put("/api/v1/workspaces/" + identifier, stale)
            .statusCode(409)
            .body("code", equalTo("GLOBAL-0009"));
    }

    @Test
    void shouldRejectDuplicateNameAfterNormalization() {
        create("Workspace Único G3", "ADMIN");

        post(validCreate("  Workspace Único G3  ", "MANAGER"))
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("name"));
    }

    @Test
    void shouldRejectDuplicateApproverFunctionalOnCreate() {
        Map<String, Object> request = validCreate("Workspace Functional Duplicado", "ADMIN");
        request.put(
            "approvers",
            List.of(
                Map.of("functional", "F1000", "email", "first@portalmanager.com"),
                Map.of("functional", "f1000", "email", "second@portalmanager.com")
            )
        );

        post(request)
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("approvers[1].functional"))
            .body(
                "details.message",
                hasItem("O funcional informado já foi adicionado como aprovador deste workspace.")
            );
    }

    @Test
    void shouldRejectDuplicateApproverEmailOnCreate() {
        Map<String, Object> request = validCreate("Workspace Email Duplicado", "ADMIN");
        request.put(
            "approvers",
            List.of(
                Map.of("functional", "F1000", "email", "same@portalmanager.com"),
                Map.of("functional", "F2000", "email", "SAME@portalmanager.com")
            )
        );

        post(request)
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("approvers[1].email"))
            .body("details.message", hasItem("O e-mail informado já foi adicionado como aprovador deste workspace."));
    }

    @Test
    void shouldRejectDuplicateApproverFunctionalOnUpdate() {
        String identifier = create("Workspace Update Functional", "ADMIN");
        Integer version = get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");

        Map<String, Object> update = validUpdate(version, "Workspace Update Functional", "ADMIN");
        update.put(
            "approvers",
            List.of(
                Map.of("functional", "F3000", "email", "first-update@portalmanager.com"),
                Map.of("functional", "f3000", "email", "second-update@portalmanager.com")
            )
        );

        put("/api/v1/workspaces/" + identifier, update)
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("approvers[1].functional"));
    }

    @Test
    void shouldRejectDuplicateApproverEmailOnUpdate() {
        String identifier = create("Workspace Update Email", "ADMIN");
        Integer version = get("/api/v1/workspaces/" + identifier)
            .statusCode(200)
            .extract()
            .path("version");

        Map<String, Object> update = validUpdate(version, "Workspace Update Email", "ADMIN");
        update.put(
            "approvers",
            List.of(
                Map.of("functional", "F4000", "email", "same-update@portalmanager.com"),
                Map.of("functional", "F5000", "email", "SAME-UPDATE@portalmanager.com")
            )
        );

        put("/api/v1/workspaces/" + identifier, update)
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("approvers[1].email"));
    }

    @Test
    void shouldReturnValidationDetailsForInvalidPayload() {
        Map<String, Object> invalid = new LinkedHashMap<>();
        invalid.put("workspaceType", "INVALID");
        invalid.put("name", " A ");
        invalid.put("description", " curta ");
        invalid.put("requester", " x ");
        invalid.put("acronym", "TOO-LONG");
        invalid.put("emailGroup", "invalid-email");
        invalid.put("approvers", List.of());

        post(invalid)
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("workspaceType"))
            .body("details.field", hasItem("name"))
            .body("details.field", hasItem("description"))
            .body("details.field", hasItem("requester"))
            .body("details.field", hasItem("acronym"))
            .body("details.field", hasItem("emailGroup"))
            .body("details.field", hasItem("approvers"));
    }

    @Test
    void shouldRejectWorkspaceSettingsThatAreNotAJsonObject() {
        Map<String, Object> request = validCreate("Workspace Invalid Settings", "ADMIN");
        request.put("settings", "not-an-object");

        post(request).statusCode(400).body("code", equalTo("GLOBAL-0001")).body("details.field", hasItem("settings"));
    }

    @Test
    void shouldInactivateHideAndRestoreWorkspace() {
        String identifier = create("Workspace Lifecycle", "ADMIN");

        post("/api/v1/workspaces/" + identifier + "/inactivate").statusCode(204);

        get("/api/v1/workspaces/" + identifier)
            .statusCode(404)
            .body("code", equalTo("WORKSPACE-0001"));

        post("/api/v1/workspaces/" + identifier + "/restore")
            .statusCode(200)
            .body("id", nullValue())
            .body("identifier", equalTo(identifier))
            .body("name", equalTo("Workspace Lifecycle"))
            .body("lifecycle", equalTo("ACTIVE"));

        post("/api/v1/workspaces/" + identifier + "/restore")
            .statusCode(400)
            .body("code", equalTo("WORKSPACE-0002"));
    }

    @Test
    void shouldQuarantineOnlyInactiveWorkspace() {
        String identifier = create("Workspace Physical Delete", "ADMIN");

        delete("/api/v1/workspaces/" + identifier)
            .statusCode(400)
            .body("code", equalTo("WORKSPACE-0003"));

        post("/api/v1/workspaces/" + identifier + "/inactivate").statusCode(204);

        delete("/api/v1/workspaces/" + identifier).statusCode(204);

        String lifecycle = jdbcTemplate.queryForObject(
            "SELECT lifecycle_code FROM workspaces WHERE identifier = ?",
            String.class,
            identifier
        );
        org.assertj.core.api.Assertions.assertThat(lifecycle).isEqualTo("QUARANTINED");
    }

    @Test
    void shouldRejectInvalidTypeFilter() {
        get("/api/v1/workspaces?typeName=INVALID")
            .statusCode(400)
            .body("code", equalTo("GLOBAL-0001"))
            .body("details.field", hasItem("typeName"));
    }

    @Test
    void shouldReturnNotFoundForUpdateAndDeleteOfUnknownWorkspace() {
        Map<String, Object> update = validUpdate(0, "Workspace Inexistente", "ADMIN");

        put("/api/v1/workspaces/00000000-0000-0000-0000-000000000000", update)
            .statusCode(404)
            .body("code", equalTo("WORKSPACE-0001"));

        delete("/api/v1/workspaces/00000000-0000-0000-0000-000000000000")
            .statusCode(404)
            .body("code", equalTo("WORKSPACE-0001"));
    }

    private String create(String name, String type) {
        return post(validCreate(name, type)).statusCode(201).extract().path("identifier");
    }

    private Map<String, Object> validCreate(String name, String type) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workspaceType", type);
        request.put("name", name);
        request.put("description", "Descrição válida para " + name.trim());
        request.put("requester", "requester");
        request.put("acronym", "WSP");
        request.put("authorizerGroup", null);
        request.put("settings", Map.of("feature", true));
        request.put("emailGroup", "workspace@portalmanager.com");
        request.put("approvers", List.of(Map.of("functional", "F1000", "email", "approver@portalmanager.com")));
        request.put("tags", List.of());
        return request;
    }

    private Map<String, Object> validUpdate(Integer version, String name, String type) {
        Map<String, Object> request = validCreate(name, type);
        request.put("version", version);
        return request;
    }

    private ValidatableResponse get(String path) {
        return given()
            .port(port)
            .header("correlation-id", "workspace-api-it")
            .header("Authorization", "Bearer workspace-api-it")
            .accept(ContentType.JSON)
            .when()
            .get(path)
            .then();
    }

    private ValidatableResponse post(Map<String, Object> body) {
        return given()
            .port(port)
            .header("correlation-id", "workspace-api-it")
            .header("Authorization", "Bearer workspace-api-it")
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(body)
            .when()
            .post("/api/v1/workspaces")
            .then();
    }

    private ValidatableResponse post(String path) {
        return given()
            .port(port)
            .header("correlation-id", "workspace-api-it")
            .header("Authorization", "Bearer workspace-api-it")
            .accept(ContentType.JSON)
            .when()
            .post(path)
            .then();
    }

    private ValidatableResponse put(String path, Map<String, Object> body) {
        return given()
            .port(port)
            .header("correlation-id", "workspace-api-it")
            .header("Authorization", "Bearer workspace-api-it")
            .contentType(ContentType.JSON)
            .accept(ContentType.JSON)
            .body(body)
            .when()
            .put(path)
            .then();
    }

    private ValidatableResponse delete(String path) {
        return given()
            .port(port)
            .header("correlation-id", "workspace-api-it")
            .header("Authorization", "Bearer workspace-api-it")
            .accept(ContentType.JSON)
            .when()
            .delete(path)
            .then();
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
