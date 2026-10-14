package br.com.portalmanager.platform.workspace.entrypoint.web.environment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxAppender;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@TestPropertySource(properties = "platform.audit.enabled=true")
class EnvironmentAuditIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuthorizationMock authorization;

    @MockitoSpyBean
    private AuditOutboxAppender auditAppender;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update("INSERT IGNORE INTO type_authorizations (code,label,description,sort_order,is_active,settings) VALUES ('DEV','DEV','DEV',1,true,'{}')");
        jdbc.update("INSERT IGNORE INTO type_workspaces (code,label,description,sort_order,is_active,settings) VALUES ('MANAGER','Manager','Manager',1,true,'{}')");
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code,label,description,sort_order,is_active,settings) VALUES ('QUARANTINED','Quarantined','Quarantined',3,true,'{}')");
        jdbc.update("INSERT IGNORE INTO environment_types (code,name,description,root_allowed,allows_children,workspace_required,lifecycle_code,display_order,created_at,updated_at) VALUES ('DEFAULT','Default','Global',true,true,false,'ACTIVE',1,NOW(6),NOW(6)),('CUSTOM','Custom','Workspace',true,true,true,'ACTIVE',2,NOW(6),NOW(6))");
        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("PM5_OWNER"));
    }

    @Test
    void auditsAllDefaultAndWorkspaceMutationsWithFlushedVersions() {
        String workspace = workspace();
        for (String base : List.of("/api/v1/environment-defaults", "/api/v1/workspaces/" + workspace + "/environments")) {
            String eventPrefix = base.contains("environment-defaults") ? "DEFAULT_ENVIRONMENT_" : "ENVIRONMENT_";
            String typeCode = base.contains("environment-defaults") ? "DEFAULT" : "CUSTOM";
            String identifier = post(base, input("Audited environment", typeCode)).statusCode(201)
                .body("version", org.hamcrest.Matchers.equalTo(0)).extract().path("identifier");
            Map<String, Object> update = new HashMap<>(input("Updated audited environment", typeCode));
            update.put("version", 0);
            request().contentType(ContentType.JSON).body(update).put(base + "/" + identifier).then().statusCode(200)
                .body("version", org.hamcrest.Matchers.equalTo(1));
            post(base + "/" + identifier + "/inactivate", null).statusCode(204);
            assertSnapshot(eventPrefix + "DEACTIVATED", identifier, "INACTIVE", 2);
            post(base + "/" + identifier + "/restore", null).statusCode(200)
                .body("version", org.hamcrest.Matchers.equalTo(3));
            assertSnapshot(eventPrefix + "RESTORED", identifier, "ACTIVE", 3);
            update.put("version", 3);
            request().contentType(ContentType.JSON).body(update).put(base + "/" + identifier).then().statusCode(200)
                .body("version", org.hamcrest.Matchers.equalTo(4));
            post(base + "/" + identifier + "/inactivate", null).statusCode(204);
            request().delete(base + "/" + identifier).then().statusCode(204);
            assertSnapshot(eventPrefix + "DELETED", identifier, "QUARANTINED", 6);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM environments WHERE identifier=?", Integer.class, identifier)).isEqualTo(1);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceIdentifier'))=?", Integer.class, identifier)).isEqualTo(7);
        }
    }

    @Test
    void rollsBackDefaultAndWorkspaceLifecycleChangesWhenAuditFails() {
        String workspace = workspace();
        for (String base : List.of("/api/v1/environment-defaults", "/api/v1/workspaces/" + workspace + "/environments")) {
            String typeCode = base.contains("environment-defaults") ? "DEFAULT" : "CUSTOM";
            String identifier = post(base, input("Environment audit rollback", typeCode)).statusCode(201).extract().path("identifier");
            doThrow(new IllegalStateException("Forced environment audit failure")).when(auditAppender).append(any(), any());
            post(base + "/" + identifier + "/inactivate", null).statusCode(500);
            assertThat(jdbc.queryForObject("SELECT lifecycle_code FROM environments WHERE identifier=?", String.class, identifier)).isEqualTo("ACTIVE");
            jdbc.update("UPDATE environments SET lifecycle_code='INACTIVE' WHERE identifier=?", identifier);
            post(base + "/" + identifier + "/restore", null).statusCode(500);
            request().delete(base + "/" + identifier).then().statusCode(500);
            assertThat(jdbc.queryForObject("SELECT lifecycle_code FROM environments WHERE identifier=?", String.class, identifier)).isEqualTo("INACTIVE");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceIdentifier'))=?", Integer.class, identifier)).isEqualTo(1);
            reset(auditAppender);
        }
    }

    private void assertSnapshot(String event, String identifier, String lifecycle, int version) {
        String predicate = "JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))=? AND JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceIdentifier'))=?";
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.lifecycle')) FROM audit_outbox WHERE " + predicate, String.class, event, identifier)).isEqualTo(lifecycle);
        assertThat(jdbc.queryForObject("SELECT JSON_EXTRACT(payload,'$.version') FROM audit_outbox WHERE " + predicate, Integer.class, event, identifier)).isEqualTo(version);
    }

    private String workspace() {
        return post("/api/v1/workspaces", Map.of("workspaceType", "MANAGER", "name", "Audit workspace", "description", "Workspace for audit", "requester", "requester", "acronym", "AUD", "settings", Map.of(), "emailGroup", "workspace@example.com", "approvers", List.of(Map.of("functional", "F1234", "email", "approver@example.com"))))
            .statusCode(201).extract().path("identifier");
    }

    private Map<String, Object> input(String name, String typeCode) {
        return Map.of("name", name, "description", "Audited environment", "authorizationType", "DEV", "environmentTypeCode", typeCode, "settings", Map.of("enabled", true));
    }

    private RequestSpecification request() {
        return given().port(port).header("correlation-id", "environment-audit-it")
            .header("Authorization", "Bearer environment-audit-it").accept(ContentType.JSON);
    }

    private ValidatableResponse post(String path, Object body) {
        RequestSpecification request = request();
        if (body != null) request.contentType(ContentType.JSON).body(body);
        return request.post(path).then();
    }
}
