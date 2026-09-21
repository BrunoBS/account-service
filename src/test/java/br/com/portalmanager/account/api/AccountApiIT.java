package br.com.portalmanager.account.api;

import br.com.portalmanager.core.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.core.testing.annotation.WithMySql;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.ArrayList;
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
class AccountApiIT {

    @LocalServerPort
    private int port;

    @Test
    void shouldCreateNormalizeAndReadActiveAccount() {
        Map<String, Object> request = validCreate("  Conta G3  ", " admin ");
        request.put("description", "  Descrição válida da conta G3  ");
        request.put("requester", "  requester  ");
        request.put("acronym", " G3 ");
        request.put("authorizerGroup", "  GRP_ACCOUNT  ");
        request.put("emailGroup", "  account@portalmanager.com  ");
        request.put("approvers", List.of(Map.of(
                "functional", "  F1234  ",
                "email", "  approver@portalmanager.com  "
        )));

        Integer id = post(request)
                .statusCode(201)
                .body("id", not(nullValue()))
                .body("version", equalTo(0))
                .body("accountType", equalTo("ADMIN"))
                .body("name", equalTo("Conta G3"))
                .body("description", equalTo("Descrição válida da conta G3"))
                .body("requester", equalTo("requester"))
                .body("acronym", equalTo("G3"))
                .body("authorizerGroup", equalTo("GRP_ACCOUNT"))
                .body("emailGroup", equalTo("account@portalmanager.com"))
                .body("lifecycle", equalTo("ACTIVE"))
                .body("onboarding", equalTo(false))
                .body("approvers", hasSize(1))
                .body("approvers[0].functional", equalTo("F1234"))
                .extract()
                .path("id");

        get("/api/v1/accounts/" + id)
                .statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo("Conta G3"))
                .body("lifecycle", equalTo("ACTIVE"));
    }

    @Test
    void shouldListByLifecycleAndNormalizedTypeFilter() {
        Integer adminId = create("Conta Admin", "ADMIN");
        Integer managerId = create("Conta Manager", "MANAGER");

        delete("/api/v1/accounts/" + managerId).statusCode(204);

        given()
                .port(port)
                .accept(ContentType.JSON)
                .queryParam("active", true)
                .queryParam("typeName", " admin ")
                .when()
                .get("/api/v1/accounts")
                .then()
                .statusCode(200)
                .body("id", hasItem(adminId))
                .body("id", not(hasItem(managerId)))
                .body("accountType", containsInAnyOrder("ADMIN"));

        get("/api/v1/accounts?active=false")
                .statusCode(200)
                .body("id", containsInAnyOrder(managerId))
                .body("lifecycle", containsInAnyOrder("INACTIVE"));
    }

    @Test
    void shouldUpdateAccountAndReplaceApprovers() {
        Integer id = create("Conta Atualizável", "ADMIN");
        Integer version = get("/api/v1/accounts/" + id)
                .statusCode(200)
                .extract()
                .path("version");

        Map<String, Object> update = validUpdate(version, "Conta Atualizável", "MANAGER");
        update.put("description", "Descrição atualizada com sucesso");
        update.put("approvers", List.of(
                Map.of("functional", "F2000", "email", "new1@portalmanager.com"),
                Map.of("functional", "F3000", "email", "new2@portalmanager.com")
        ));

        put("/api/v1/accounts/" + id, update)
                .statusCode(200)
                .body("id", equalTo(id))
                .body("version", greaterThan(version))
                .body("name", equalTo("Conta Atualizável"))
                .body("accountType", equalTo("MANAGER"))
                .body("description", equalTo("Descrição atualizada com sucesso"))
                .body("approvers", hasSize(2))
                .body("approvers.functional", containsInAnyOrder("F2000", "F3000"));
    }

    @Test
    void shouldRejectStaleUpdateWithConflict() {
        Integer id = create("Conta Concorrente", "ADMIN");
        Integer version = get("/api/v1/accounts/" + id)
                .statusCode(200)
                .extract()
                .path("version");

        Map<String, Object> first = validUpdate(version, "Conta Concorrente", "ADMIN");
        first.put("description", "Descrição da primeira atualização");
        put("/api/v1/accounts/" + id, first).statusCode(200);

        Map<String, Object> stale = validUpdate(version, "Conta Concorrente", "ADMIN");
        stale.put("description", "Descrição da atualização obsoleta");

        put("/api/v1/accounts/" + id, stale)
                .statusCode(409)
                .body("code", equalTo("GLOBAL-0009"));
    }

    @Test
    void shouldRejectDuplicateNameAfterNormalization() {
        create("Conta Única G3", "ADMIN");

        post(validCreate("  Conta Única G3  ", "MANAGER"))
                .statusCode(400)
                .body("code", equalTo("GLOBAL-0001"))
                .body("details.field", hasItem("name"));
    }

    @Test
    void shouldReturnValidationDetailsForInvalidPayload() {
        Map<String, Object> invalid = new LinkedHashMap<>();
        invalid.put("accountType", "CATALOG");
        invalid.put("name", " A ");
        invalid.put("description", " curta ");
        invalid.put("requester", " x ");
        invalid.put("acronym", "TOO-LONG");
        invalid.put("emailGroup", "invalid-email");
        invalid.put("approvers", List.of());

        post(invalid)
                .statusCode(400)
                .body("code", equalTo("GLOBAL-0001"))
                .body("details.field", hasItem("accountType"))
                .body("details.field", hasItem("name"))
                .body("details.field", hasItem("description"))
                .body("details.field", hasItem("requester"))
                .body("details.field", hasItem("acronym"))
                .body("details.field", hasItem("emailGroup"))
                .body("details.field", hasItem("approvers"));
    }

    @Test
    void shouldDeactivateHideAndRestoreAccount() {
        Integer id = create("Conta Lifecycle", "ADMIN");

        delete("/api/v1/accounts/" + id).statusCode(204);

        get("/api/v1/accounts/" + id)
                .statusCode(404)
                .body("code", equalTo("GLOBAL-0005"));

        post("/api/v1/accounts/" + id + "/restore")
                .statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo("Conta Lifecycle"))
                .body("lifecycle", equalTo("ACTIVE"));

        post("/api/v1/accounts/" + id + "/restore")
                .statusCode(400)
                .body("code", equalTo("GLOBAL-0001"));
    }

    @Test
    void shouldRejectInvalidTypeFilter() {
        get("/api/v1/accounts?typeName=CATALOG")
                .statusCode(400)
                .body("code", equalTo("GLOBAL-0001"))
                .body("details.field", hasItem("typeName"));
    }

    @Test
    void shouldReturnNotFoundForUpdateAndDeleteOfUnknownAccount() {
        Map<String, Object> update = validUpdate(0, "Conta Inexistente", "ADMIN");

        put("/api/v1/accounts/999999", update)
                .statusCode(404)
                .body("code", equalTo("GLOBAL-0005"));

        delete("/api/v1/accounts/999999")
                .statusCode(404)
                .body("code", equalTo("GLOBAL-0005"));
    }

    private Integer create(String name, String type) {
        return post(validCreate(name, type))
                .statusCode(201)
                .extract()
                .path("id");
    }

    private Map<String, Object> validCreate(String name, String type) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("accountType", type);
        request.put("name", name);
        request.put("description", "Descrição válida para " + name.trim());
        request.put("requester", "requester");
        request.put("acronym", "ACC");
        request.put("authorizerGroup", null);
        request.put("settings", "{\"feature\":true}");
        request.put("emailGroup", "account@portalmanager.com");
        request.put("approvers", List.of(Map.of(
                "functional", "F1000",
                "email", "approver@portalmanager.com"
        )));
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
                .accept(ContentType.JSON)
                .when()
                .get(path)
                .then();
    }

    private ValidatableResponse post(Map<String, Object> body) {
        return given()
                .port(port)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/v1/accounts")
                .then();
    }

    private ValidatableResponse post(String path) {
        return given()
                .port(port)
                .accept(ContentType.JSON)
                .when()
                .post(path)
                .then();
    }

    private ValidatableResponse put(String path, Map<String, Object> body) {
        return given()
                .port(port)
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
                .accept(ContentType.JSON)
                .when()
                .delete(path)
                .then();
    }
}
