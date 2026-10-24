package br.com.portalmanager.platform.workspace.entrypoint.workspace;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.http.PlatformRequestSpecificationFactory;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class WorkspaceAuditIT {

    @Autowired
    private AuthorizationMock authorizationMock;

    @Autowired
    private PlatformRequestSpecificationFactory requests;

    private WorkspaceClient client;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void auditProperties(DynamicPropertyRegistry registry) {
        registry.add("platform.audit.enabled", () -> true);
        registry.add("platform.audit.service-name", () -> "workspace-service");
    }

    @BeforeEach
    void setUp() {
        client = new WorkspaceClient(requests, JsonMapper.builder().build(), "workspace-audit-it", "workspace-audit-request");
        SchemaDefaultFixture.seed(jdbcTemplate);
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings) VALUES ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}')"
        );
        jdbcTemplate.update(
            "INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings) VALUES ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}')"
        );
        authorizationMock.reset();
        authorizationMock.allow(session ->
            session
                .groups("PM5_OWNER")
                .userName("auditor")
                .accountId("audit-account")
                .applicationId("audit-application")
                .environmentId("audit-environment")
                .traceId("audit-trace")
        );
        jdbcTemplate.update("DELETE FROM audit_outbox");
    }

    @Test
    void shouldAuditWorkspaceMutations() {
        ExtractableResponse<Response> created = post(validCreate("Workspace Audit")).statusCode(201).extract();

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
            "WORKSPACE_CREATED",
            "WORKSPACE_UPDATED",
            "WORKSPACE_DEACTIVATED",
            "WORKSPACE_RESTORED",
            "WORKSPACE_DEACTIVATED",
            "WORKSPACE_DELETED"
        );
        assertThat(jsonValues("$.resourceType")).containsOnly("WORKSPACE");
        assertThat(jsonValues("$.resourceIdentifier")).containsOnly(identifier);
        assertThat(jsonValues("$.service")).containsOnly("workspace-service");
        assertThat(jsonValues("$.username")).containsOnly("auditor");
        assertThat(jsonValues("$.correlationId")).containsOnly("audit-trace");
        List<String> deactivationSnapshots = jdbcTemplate.queryForList(
            "SELECT CAST(payload AS CHAR) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.eventType')) = 'WORKSPACE_DEACTIVATED' ORDER BY id",
            String.class
        );
        assertThat(deactivationSnapshots).hasSize(2);
        tools.jackson.databind.json.JsonMapper mapper = tools.jackson.databind.json.JsonMapper.builder().build();
        for (String snapshot : deactivationSnapshots) {
            tools.jackson.databind.JsonNode payload = mapper.readTree(snapshot);
            assertThat(payload.get("identifier").asText()).isEqualTo(identifier);
            assertThat(payload.get("lifecycle").asText()).isEqualTo("INACTIVE");
            assertThat(payload.get("tags").get(0).asText()).isEqualTo("audit-manual");
            assertThat(payload.get("settings").get("feature").asBoolean()).isTrue();
            assertThat(payload.get("approvers").get(0).get("functional").asText()).isEqualTo("F1000");
        }
        String restorationSnapshot = jdbcTemplate.queryForObject(
            "SELECT CAST(payload AS CHAR) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.eventType')) = 'WORKSPACE_RESTORED'",
            String.class
        );
        tools.jackson.databind.JsonNode restoredPayload = mapper.readTree(restorationSnapshot);
        assertThat(restoredPayload.get("lifecycle").asText()).isEqualTo("ACTIVE");
        assertThat(restoredPayload.get("tags").get(0).asText()).isEqualTo("audit-manual");
        String deletionSnapshot = jdbcTemplate.queryForObject(
            "SELECT CAST(payload AS CHAR) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata, '$.eventType')) = 'WORKSPACE_DELETED'",
            String.class
        );
        tools.jackson.databind.JsonNode deletedPayload = mapper.readTree(deletionSnapshot);
        assertThat(deletedPayload.get("identifier").asText()).isEqualTo(identifier);
        assertThat(deletedPayload.get("lifecycle").asText()).isEqualTo("QUARANTINED");
        assertThat(deletedPayload.get("tags").get(0).asText()).isEqualTo("audit-manual");
        assertThat(deletedPayload.get("settings").get("feature").asBoolean()).isTrue();
        assertThat(deletedPayload.get("approvers").get(0).get("functional").asText()).isEqualTo("F1000");
        post("/api/v1/workspaces/" + identifier + "/restore").statusCode(400);
        delete(identifier).statusCode(400);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isEqualTo(6);
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
        request.put("approvers", List.of(Map.of("functional", "F1000", "email", "approver@portalmanager.com")));
        request.put("tags", List.of("audit-manual"));
        return request;
    }

    private io.restassured.response.ValidatableResponse post(Map<String, Object> body) {
        return client.create(body).response();
    }

    private io.restassured.response.ValidatableResponse post(String path) {
        return client.action(path).response();
    }

    private io.restassured.response.ValidatableResponse put(String identifier, Map<String, Object> body) {
        return client.updateByIdentifier(identifier, body).response();
    }

    private io.restassured.response.ValidatableResponse delete(String identifier) {
        return client.deleteByIdentifier(identifier).response();
    }

}
