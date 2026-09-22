package br.com.itau.portalmanager.workspace.input.web.workspace;

import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
import br.com.portalmanager.platform.testing.authorization.AuthorizationMock;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class WorkspaceApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthorizationMock authorizationMock;

    @BeforeEach
    void authorizeAsOwner() {
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
        request.put("approvers", List.of(Map.of(
                "functional", "  F1234  ",
                "email", "  approver@portalmanager.com  "
        )));

        Integer id = post(request)
                .statusCode(201)
                .body("id", not(nullValue()))
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
                .path("id");

        get("/api/v1/workspaces/" + id)
                .statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo("Workspace G3"))
                .body("lifecycle", equalTo("ACTIVE"));
    }

    @Test
    void shouldListByLifecycleAndNormalizedTypeFilter() {
        Integer adminId = create("Workspace Admin", "ADMIN");
        Integer managerId = create("Workspace Manager", "MANAGER");

        delete("/api/v1/workspaces/" + managerId).statusCode(204);

        given()
                .port(port)
                .header("X-Correlation-Id", "workspace-api-it")
                .header("Authorization", "Bearer workspace-api-it")
                .accept(ContentType.JSON)
                .queryParam("active", true)
                .queryParam("typeName", " admin ")
                .when()
                .get("/api/v1/workspaces")
                .then()
                .statusCode(200)
                .body("id", hasItem(adminId))
                .body("id", not(hasItem(managerId)))
                .body("workspaceType", containsInAnyOrder("ADMIN"));

        get("/api/v1/workspaces?active=false")
                .statusCode(200)
                .body("id", containsInAnyOrder(managerId))
                .body("lifecycle", containsInAnyOrder("INACTIVE"));
    }

    @Test
    void shouldUpdateWorkspaceAndReplaceApprovers() {
        Integer id = create("Workspace Atualizável", "ADMIN");
        Integer version = get("/api/v1/workspaces/" + id)
                .statusCode(200)
                .extract()
                .path("version");

        Map<String, Object> update = validUpdate(version, "Workspace Atualizável", "MANAGER");
        update.put("description", "Descrição atualizada com sucesso");
        update.put("approvers", List.of(
                Map.of("functional", "F2000", "email", "new1@portalmanager.com"),
                Map.of("functional", "F3000", "email", "new2@portalmanager.com")
        ));

        put("/api/v1/workspaces/" + id, update)
                .statusCode(200)
                .body("id", equalTo(id))
                .body("version", greaterThan(version))
                .body("name", equalTo("Workspace Atualizável"))
                .body("workspaceType", equalTo("MANAGER"))
                .body("description", equalTo("Descrição atualizada com sucesso"))
                .body("approvers", hasSize(2))
                .body("approvers.functional", containsInAnyOrder("F2000", "F3000"));
    }

    @Test
    void shouldRejectStaleUpdateWithConflict() {
        Integer id = create("Workspace Concorrente", "ADMIN");
        Integer version = get("/api/v1/workspaces/" + id)
                .statusCode(200)
                .extract()
                .path("version");

        Map<String, Object> first = validUpdate(version, "Workspace Concorrente", "ADMIN");
        first.put("description", "Descrição da primeira atualização");
        put("/api/v1/workspaces/" + id, first).statusCode(200);

        Map<String, Object> stale = validUpdate(version, "Workspace Concorrente", "ADMIN");
        stale.put("description", "Descrição da atualização obsoleta");

        put("/api/v1/workspaces/" + id, stale)
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
    void shouldInactivateHideAndRestoreWorkspace() {
        Integer id = create("Workspace Lifecycle", "ADMIN");

        delete("/api/v1/workspaces/" + id).statusCode(204);

        get("/api/v1/workspaces/" + id)
                .statusCode(404)
                .body("code", equalTo("WORKSPACE-0001"));

        post("/api/v1/workspaces/" + id + "/restore")
                .statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo("Workspace Lifecycle"))
                .body("lifecycle", equalTo("ACTIVE"));

        post("/api/v1/workspaces/" + id + "/restore")
                .statusCode(400)
                .body("code", equalTo("WORKSPACE-0002"));
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

        put("/api/v1/workspaces/999999", update)
                .statusCode(404)
                .body("code", equalTo("WORKSPACE-0001"));

        delete("/api/v1/workspaces/999999")
                .statusCode(404)
                .body("code", equalTo("WORKSPACE-0001"));
    }

    private Integer create(String name, String type) {
        return post(validCreate(name, type))
                .statusCode(201)
                .extract()
                .path("id");
    }

    private Map<String, Object> validCreate(String name, String type) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workspaceType", type);
        request.put("name", name);
        request.put("description", "Descrição válida para " + name.trim());
        request.put("requester", "requester");
        request.put("acronym", "WSP");
        request.put("authorizerGroup", null);
        request.put("settings", "{\"feature\":true}");
        request.put("emailGroup", "workspace@portalmanager.com");
        request.put("approvers", List.of(Map.of(
                "functional", "F1000",
                "email", "approver@portalmanager.com"
        )));
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
                .header("X-Correlation-Id", "workspace-api-it")
                .header("Authorization", "Bearer workspace-api-it")
                .accept(ContentType.JSON)
                .when()
                .get(path)
                .then();
    }

    private ValidatableResponse post(Map<String, Object> body) {
        return given()
                .port(port)
                .header("X-Correlation-Id", "workspace-api-it")
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
                .header("X-Correlation-Id", "workspace-api-it")
                .header("Authorization", "Bearer workspace-api-it")
                .accept(ContentType.JSON)
                .when()
                .post(path)
                .then();
    }

    private ValidatableResponse put(String path, Map<String, Object> body) {
        return given()
                .port(port)
                .header("X-Correlation-Id", "workspace-api-it")
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
                .header("X-Correlation-Id", "workspace-api-it")
                .header("Authorization", "Bearer workspace-api-it")
                .accept(ContentType.JSON)
                .when()
                .delete(path)
                .then();
    }
}
