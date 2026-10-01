package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import br.com.portalmanager.platform.library.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
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
        SchemaDefaultFixture.seed(jdbc);
        for (String code : new String[]{"ACTIVE", "INACTIVE", "QUARANTINED"})
            jdbc.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES (?, ?, ?, 1, true, '{}')",
                    code, code, code);
        for (String code : new String[]{"DEFAULT", "CUSTOM", "SHARD", "CELL"}) {
            boolean rootAllowed = code.equals("DEFAULT") || code.equals("CUSTOM");
            boolean workspaceRequired = !code.equals("DEFAULT");
            jdbc.update("""
                    INSERT IGNORE INTO environment_types
                      (identifier, code, name, description, root_allowed, workspace_required,
                       lifecycle_code, display_order, created_at, updated_at)
                    VALUES (UUID(), ?, ?, ?, ?, ?, 'ACTIVE', 1, NOW(6), NOW(6))
                    """, code, code, code + " environment", rootAllowed, workspaceRequired);
        }
        jdbc.update("""
                INSERT IGNORE INTO environment_type_compatibilities
                  (identifier, parent_type_id, child_type_id, lifecycle_code, created_at, updated_at)
                SELECT UUID(), p.id, c.id, 'ACTIVE', NOW(6), NOW(6)
                FROM environment_types p JOIN environment_types c
                WHERE (p.code = 'DEFAULT' AND c.code = 'SHARD')
                   OR (p.code = 'CUSTOM' AND c.code = 'SHARD')
                   OR (p.code = 'SHARD' AND c.code = 'CELL')
                """);
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

    @Test
    void createsWorkspaceHierarchyAndKeepsChildrenIsolated() {
        String a = workspace();
        String b = workspace();
        String global = post("/api/v1/environment-defaults", input("Global " + UUID.randomUUID()))
                .statusCode(201).extract().path("identifier");
        String baseA = "/api/v1/workspaces/" + a + "/environments";
        String baseB = "/api/v1/workspaces/" + b + "/environments";
        String shardA = post(baseA, child("SHARD A", "SHARD", global))
                .statusCode(201).body("environmentType", equalTo("SHARD"))
                .body("parentIdentifier", equalTo(global)).extract().path("identifier");
        String shardB = post(baseB, child("SHARD B", "SHARD", global))
                .statusCode(201).extract().path("identifier");
        String cellA = post(baseA, child("CELL A", "CELL", shardA))
                .statusCode(201).extract().path("identifier");

        get(baseA + "/" + global + "/children").statusCode(200).body("identifier", hasItem(shardA))
                .body("identifier", not(hasItem(shardB)));
        get(baseB + "/" + shardA).statusCode(404);
        post(baseB, child("Cross workspace", "CELL", shardA)).statusCode(404);
        post(baseA, child("Direct cell", "CELL", global)).statusCode(400);
        post(baseA, withType("Root shard", "SHARD")).statusCode(400);
        post("/api/v1/environment-defaults", withType("Global custom", "CUSTOM")).statusCode(400);
        get(baseA + "/tree").statusCode(200).body("environment.identifier", hasItem(global));

        post("/api/v1/environment-defaults/" + global + "/inactivate", null).statusCode(204);
        get(baseA + "/" + shardA).statusCode(404);
        get(baseA + "/" + cellA).statusCode(404);
        get(baseA + "/" + global + "/children").statusCode(404);
    }

    @Test
    void managesTypesWithWorkspaceRequirementAsData() {
        String base = "/api/v1/environment-types";
        post(base, Map.of()).statusCode(400)
                .body("details.field", hasItems("code", "name", "description", "rootAllowed", "workspaceRequired", "displayOrder"));
        post(base + "/compatibilities", Map.of()).statusCode(400)
                .body("details.field", hasItems("parentTypeCode", "childTypeCode"));
        String code = "REGION" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> type = Map.of("code", code, "name", "Region", "description", "Regional environment",
                "rootAllowed", false, "workspaceRequired", true, "displayOrder", 5);
        String identifier = post(base, type).statusCode(201).body("workspaceRequired", equalTo(true))
                .extract().path("identifier");
        post(base, type).statusCode(400).body("details.field", hasItem("code"));
        get(base + "/" + identifier).statusCode(200).body("code", equalTo(code));
        get(base).statusCode(200).body("code", hasItem(code));
        String workspace = workspace();
        post("/api/v1/workspaces/" + workspace + "/environments", withType("Root region", code)).statusCode(400);
        post(base + "/compatibilities", Map.of("parentTypeCode", "SHARD", "childTypeCode", code))
                .statusCode(201).body("childTypeCode", equalTo(code));
        get(base + "/compatibilities").statusCode(200).body("childTypeCode", hasItem(code));
        post(base + "/compatibilities", Map.of("parentTypeCode", code, "childTypeCode", "SHARD"))
                .statusCode(400).body("details.field", hasItem("childTypeCode"));
    }

    private String workspace() {
        return post("/api/v1/workspaces", Map.of(
                "workspaceType", "MANAGER", "name", "Environment Workspace " + UUID.randomUUID(),
                "description", "Workspace for environments", "requester", "requester",
                "acronym", "ENV", "settings", Map.of(), "emailGroup", "workspace@portalmanager.com",
                "approvers", java.util.List.of(Map.of("functional", "F1234", "email", "approver@portalmanager.com"))))
                .statusCode(201).extract().path("identifier");
    }

    private Map<String, Object> input(String name) {
        return Map.of("name", name, "description", "Environment description", "authorizationType", "DEV", "settings", Map.of());
    }

    private Map<String, Object> withType(String name, String code) {
        var result = new java.util.HashMap<String, Object>(input(name));
        result.put("environmentTypeCode", code);
        return result;
    }

    private Map<String, Object> child(String name, String code, String parent) {
        var result = new java.util.HashMap<String, Object>(withType(name, code));
        result.put("parentIdentifier", parent);
        return result;
    }

    private ValidatableResponse get(String path) {
        return given().port(port).header("correlationId", "environment-api-it")
                .header("Authorization", "Bearer environment-api-it").accept(ContentType.JSON).when().get(path).then();
    }

    private ValidatableResponse post(String path, Object body) {
        var request = given().port(port).header("correlationId", "environment-api-it")
                .header("Authorization", "Bearer environment-api-it").contentType(ContentType.JSON).accept(ContentType.JSON);
        if (body != null) request.body(body);
        return request.when().post(path).then();
    }
}
