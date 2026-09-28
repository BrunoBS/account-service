package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

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

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class EnvironmentApiIT {
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthorizationMock authorization;

    @BeforeEach
    void prepare() {
        for (String code : new String[]{"ACTIVE", "INACTIVE", "QUARANTINED"})
            jdbc.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES (?, ?, ?, 1, true, '{}')",
                    code, code, code);
        for (String code : new String[]{"DEFAULT", "CUSTOM"})
            jdbc.update("INSERT IGNORE INTO type_environments (code, label, description, sort_order, is_active, settings) VALUES (?, ?, ?, 1, true, '{}')",
                    code, code, code);
        for (String code : new String[]{"DEV", "TST", "ADM"})
            jdbc.update("INSERT IGNORE INTO type_authorizations (code, label, description, sort_order, is_active, settings) VALUES (?, ?, ?, 1, true, '{}')",
                    code, code, code);
        jdbc.update("INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('MANAGER', 'Manager', 'Manager', 1, true, '{}')");
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void keepsDefaultsGlobalAndCustomEnvironmentsScopedToWorkspace() {
        String a = workspace();
        String b = workspace();
        String defaults = "/api/v1/environment-defaults";
        String customA = "/api/v1/workspaces/" + a + "/environments";
        String customB = "/api/v1/workspaces/" + b + "/environments";
        String name = "Shared " + UUID.randomUUID();
        String global = post(defaults, input(name)).statusCode(201).body("environmentType", equalTo("DEFAULT"))
                .body("sortOrder", greaterThanOrEqualTo(1)).extract().path("identifier");
        String first = post(customA, input(name)).statusCode(201).body("environmentType", equalTo("CUSTOM"))
                .body("sortOrder", greaterThanOrEqualTo(4)).extract().path("identifier");
        String second = post(customB, input(name)).statusCode(201).extract().path("identifier");
        post(defaults, input(name)).statusCode(400).body("details.field", hasItem("name"));
        post(customA, input(name)).statusCode(400).body("details.field", hasItem("name"));
        get(customA + "/" + second).statusCode(404);
        get(customB + "/" + first).statusCode(404);
        get(defaults + "/" + first).statusCode(404);
        get(defaults + "/" + global).statusCode(200);
        post(customA + "/" + first + "/inactivate", null).statusCode(204);
        get(customA + "/" + first).statusCode(404);
        get(customA + "?active=false").statusCode(200).body("identifier", hasItem(first));
        post(customA + "/" + first + "/restore", null).statusCode(200).body("lifecycle", equalTo("ACTIVE"));
        post(defaults + "/" + global + "/inactivate", null).statusCode(204);
        post(defaults + "/" + global + "/restore", null).statusCode(200);
    }

    private String workspace() {
        return post("/api/v1/workspaces", Map.of(
                "workspaceType", "MANAGER", "name", "Environment Workspace " + UUID.randomUUID(),
                "description", "Workspace for environments", "requester", "requester",
                "acronym", "ENV", "settings", "{}", "emailGroup", "workspace@portalmanager.com",
                "approvers", java.util.List.of(Map.of("functional", "F1234", "email", "approver@portalmanager.com"))))
                .statusCode(201).extract().path("identifier");
    }

    private Map<String, Object> input(String name) {
        return Map.of("name", name, "description", "Environment description", "authorizationType", "DEV", "settings", "{}");
    }

    private ValidatableResponse get(String path) {
        return given().port(port).header("X-Correlation-Id", "environment-api-it")
                .header("Authorization", "Bearer environment-api-it").accept(ContentType.JSON).when().get(path).then();
    }

    private ValidatableResponse post(String path, Object body) {
        var request = given().port(port).header("X-Correlation-Id", "environment-api-it")
                .header("Authorization", "Bearer environment-api-it").contentType(ContentType.JSON).accept(ContentType.JSON);
        if (body != null) request.body(body);
        return request.when().post(path).then();
    }
}
