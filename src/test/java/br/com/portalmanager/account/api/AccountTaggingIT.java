package br.com.portalmanager.account.api;

import br.com.portalmanager.core.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.core.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.core.testing.annotation.WithMySql;
import br.com.portalmanager.core.testing.authorization.AuthorizationMock;
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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class AccountTaggingIT {

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
    void shouldNormalizeExposeManualTagsAndHideSystemTags() {
        Map<String, Object> request = validCreate("Conta Tags", "TAG", "TEAM_A");
        request.put("tags", List.of("  Minha   Tag  ", "minha\tTag", "OUTRA TAG", "   "));

        Integer id = post(request)
                .statusCode(201)
                .body("tags", containsInAnyOrder("minha-tag", "outra-tag"))
                .body("tags", not(hasItem("conta-tags")))
                .extract()
                .path("id");

        getList("Minha Tag")
                .statusCode(200)
                .body("id", hasItem(id));

        getList("Conta Tags")
                .statusCode(200)
                .body("id", hasItem(id));

        getList("TEAM_A")
                .statusCode(200)
                .body("id", hasItem(id));
    }

    @Test
    void shouldRecalculateSystemTagsOnUpdateAndPreserveManualTags() {
        Map<String, Object> create = validCreate("Conta Original", "ORG", "TEAM_A");
        create.put("tags", List.of("manual-tag"));

        var created = post(create).statusCode(201).extract();
        Integer id = created.path("id");
        Integer version = created.path("version");

        Map<String, Object> update = validUpdate(version, "Conta Atualizada", "ATU", "TEAM_B");
        update.put("tags", List.of("manual-tag"));

        put(id, update)
                .statusCode(200)
                .body("tags", containsInAnyOrder("manual-tag"));

        getList("Conta Original")
                .statusCode(200)
                .body("id", not(hasItem(id)));

        getList("ORG")
                .statusCode(200)
                .body("id", not(hasItem(id)));

        getList("Conta Atualizada")
                .statusCode(200)
                .body("id", hasItem(id));

        getList("ATU")
                .statusCode(200)
                .body("id", hasItem(id));

        getList("manual-tag")
                .statusCode(200)
                .body("id", hasItem(id));
    }

    private Map<String, Object> validCreate(String name, String acronym, String authorizerGroup) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("accountType", "ADMIN");
        request.put("name", name);
        request.put("description", "Descrição válida para " + name);
        request.put("requester", "requester");
        request.put("acronym", acronym);
        request.put("authorizerGroup", authorizerGroup);
        request.put("settings", "{\"feature\":true}");
        request.put("emailGroup", "account@portalmanager.com");
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

    private io.restassured.response.ValidatableResponse getList(String tagName) {
        return authorized()
                .queryParam("tagName", tagName)
                .when()
                .get("/api/v1/accounts")
                .then();
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
                .port(port)
                .header("X-Correlation-Id", "tagging-it")
                .header("Authorization", "Bearer tagging-it")
                .accept(ContentType.JSON);
    }
}
