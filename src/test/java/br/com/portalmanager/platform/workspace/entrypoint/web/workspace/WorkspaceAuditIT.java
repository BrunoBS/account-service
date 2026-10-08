package br.com.portalmanager.platform.workspace.entrypoint.web.workspace;

import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class WorkspaceAuditIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthorizationMock authorizationMock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void auditProperties(DynamicPropertyRegistry registry) {
        registry.add("platform.audit.enabled", () -> true);
        registry.add("platform.audit.service-name", () -> "workspace-service");
    }

    @BeforeEach
    void setUp() {
        SchemaDefaultFixture.seed(jdbcTemplate);
        jdbcTemplate.update("INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}')");
        jdbcTemplate.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')");
        jdbcTemplate.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}')");
        jdbcTemplate.update("INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}')");
        authorizationMock.reset();
        authorizationMock.allow(session -> session
                .groups("PM5_OWNER")
                .userName("golden-auditor")
                .accountId("audit-account")
                .applicationId("audit-application")
                .environmentId("audit-environment")
                .traceId("audit-trace"));
        jdbcTemplate.update("DELETE FROM audit_outbox");
    }

    @Test
    void shouldAuditWorkspaceMutations() {
        var created = post(validCreate("Workspace Audit"))
                .statusCode(201)
                .extract();

        String identifier = created.path("identifier");
        Integer version = created.path("version");

        Map<String, Object> update = validCreate("Workspace Audit");
        update.put("version", version);
        update.put("description", "Descrição atualizada para auditoria");

        put(identifier, update).statusCode(200);
        post("/api/v1/workspaces/" + identifier + "/inactivate").statusCode(204);
        post("/api/v1/workspaces/" + identifier + "/restore").statusCode(200);
        post("/api/v1/workspaces/" + identifier + "/inactivate").statusCode(204);
        delete(identifier).statusCode(204);

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isEqualTo(6);
        assertThat(jsonValues("$.eventType")).containsExactly(
                "WORKSPACE_CREATED", "WORKSPACE_UPDATED", "WORKSPACE_DEACTIVATED",
                "WORKSPACE_RESTORED", "WORKSPACE_DEACTIVATED", "WORKSPACE_DELETED");
        assertThat(jsonValues("$.resourceType")).containsOnly("WORKSPACE");
        assertThat(jsonValues("$.resourceIdentifier")).containsOnly(identifier);
        assertThat(jsonValues("$.service")).containsOnly("workspace-service");
        assertThat(jsonValues("$.username")).containsOnly("golden-auditor");
        assertThat(jsonValues("$.correlationId")).containsOnly("audit-trace");
    }

    private List<String> jsonValues(String path) {
        return jdbcTemplate.queryForList(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(metadata, '" + path + "')) FROM audit_outbox ORDER BY id",
                String.class
        );
    }

    private Map<String, Object> validCreate(String name) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workspaceType", "ADMIN");
        request.put("name", name);
        request.put("description", "Descrição válida para " + name);
        request.put("requester", "requester");
        request.put("acronym", "AUD");
        request.put("authorizerGroup", "AUDIT_TEAM");
        request.put("settings", Map.of("feature", true));
        request.put("emailGroup", "workspace@portalmanager.com");
        request.put("approvers", List.of(Map.of(
                "functional", "F1000",
                "email", "approver@portalmanager.com"
        )));
        request.put("tags", List.of("audit-manual"));
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

    private io.restassured.response.ValidatableResponse post(String path) {
        return authorized().when().post(path).then();
    }

    private io.restassured.response.ValidatableResponse put(String identifier, Map<String, Object> body) {
        return authorized()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/api/v1/workspaces/" + identifier)
                .then();
    }

    private io.restassured.response.ValidatableResponse delete(String identifier) {
        return authorized().when().delete("/api/v1/workspaces/" + identifier).then();
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
                .port(port)
                .header("correlationId", "workspace-audit-request")
                .header("Authorization", "Bearer workspace-audit-it")
                .accept(ContentType.JSON);
    }
}
