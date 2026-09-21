package br.com.portalmanager.account.api;

import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
import br.com.portalmanager.platform.testing.authorization.AuthorizationMock;
import io.restassured.http.ContentType;
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

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class AccountAuthorizationIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthorizationMock authorizationMock;

    @BeforeEach
    void resetAsOwner() {
        allowOwner();
    }

    @Test
    void shouldRequireAuthenticationEvenForOpenPolicy() {
        given()
                .port(port)
                .accept(ContentType.JSON)
                .when()
                .get("/api/v1/accounts")
                .then()
                .statusCode(401)
                .body("code", equalTo("AUTH-401-001"));
    }

    @Test
    void shouldApplyOpenDevAndAdmPolicies() {
        Integer id = post(validCreate("Conta Policy", "TEAM_POLICY"))
                .statusCode(201)
                .extract()
                .path("id");
        authorizationMock.verifyCalledWithPolicy("OPEN");

        allowOwner();
        Integer version = get("/api/v1/accounts/" + id)
                .statusCode(200)
                .extract()
                .path("version");
        authorizationMock.verifyCalledWithPolicy("DEV");

        allowOwner();
        Map<String, Object> update = validCreate("Conta Policy", "TEAM_POLICY");
        update.put("version", version);
        update.put("description", "Descrição atualizada para policy");

        put(id, update).statusCode(200);
        authorizationMock.verifyCalledWithPolicy("ADM");
    }

    @Test
    void shouldApplyResourceVisibilityForSingleAndCollectionReads() {
        Integer teamA = post(validCreate("Conta Team A", "TEAM_A"))
                .statusCode(201)
                .extract()
                .path("id");

        Integer teamB = post(validCreate("Conta Team B", "TEAM_B"))
                .statusCode(201)
                .extract()
                .path("id");

        authorizationMock.reset();
        authorizationMock.allow(session -> session
                .groups("USER")
                .addAuthorizerGroup("GRP_ACCOUNT_DEV_TEAM_A", "DEV", "DEV", "TEAM_A"));

        get("/api/v1/accounts/" + teamA)
                .statusCode(200)
                .body("id", equalTo(teamA));

        get("/api/v1/accounts")
                .statusCode(200)
                .body("id", containsInAnyOrder(teamA));

        get("/api/v1/accounts/" + teamB)
                .statusCode(403)
                .body("code", equalTo("AUTH-403-004"));

        allowOwner();
        get("/api/v1/accounts/" + teamB)
                .statusCode(200)
                .body("id", equalTo(teamB));
    }

    private void allowOwner() {
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("PM5_OWNER"));
    }

    private Map<String, Object> validCreate(String name, String authorizerGroup) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("accountType", "ADMIN");
        request.put("name", name);
        request.put("description", "Descrição válida para " + name);
        request.put("requester", "requester");
        request.put("acronym", "AUTH");
        request.put("authorizerGroup", authorizerGroup);
        request.put("settings", "{\"feature\":true}");
        request.put("emailGroup", "account@portalmanager.com");
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
                .post("/api/v1/accounts")
                .then();
    }

    private io.restassured.response.ValidatableResponse put(Integer id, Map<String, Object> body) {
        return authorized()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/api/v1/accounts/" + id)
                .then();
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
                .port(port)
                .header("X-Correlation-Id", "authorization-it")
                .header("Authorization", "Bearer authorization-it")
                .accept(ContentType.JSON);
    }
}
