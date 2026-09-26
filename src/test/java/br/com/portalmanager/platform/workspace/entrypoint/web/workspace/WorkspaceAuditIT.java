package br.com.portalmanager.platform.workspace.entrypoint.web.workspace;

import br.com.portalmanager.platform.library.audit.model.AuditEventRequest;
import br.com.portalmanager.platform.library.audit.publisher.AuditPublisher;
import br.com.portalmanager.platform.library.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@Import(WorkspaceAuditIT.AuditCaptureConfiguration.class)
class WorkspaceAuditIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthorizationMock authorizationMock;

    @Autowired
    private CapturingAuditPublisher auditPublisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void auditProperties(DynamicPropertyRegistry registry) {
        registry.add("platform.audit.enabled", () -> true);
        registry.add("platform.audit.service-name", () -> "workspace-service");
    }

    @BeforeEach
    void setUp() {
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
        auditPublisher.clear();
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

        List<AuditEventRequest> events = new ArrayList<>(auditPublisher.events());

        assertThat(events).hasSize(6);
        assertThat(events)
                .extracting(AuditEventRequest::resource)
                .containsOnly("WORKSPACE");
        assertThat(events)
                .extracting(AuditEventRequest::action)
                .containsExactly("INSERT", "UPDATE", "INACTIVATE", "RESTORE", "INACTIVATE", "DELETE");
        assertThat(events)
                .extracting(AuditEventRequest::resourceId)
                .containsOnly(identifier);
        assertThat(events)
                .extracting(AuditEventRequest::service)
                .containsOnly("workspace-service");
        assertThat(events)
                .extracting(AuditEventRequest::actor)
                .containsOnly("golden-auditor");
        assertThat(events)
                .extracting(AuditEventRequest::correlationId)
                .containsOnly("audit-trace");
    }

    private Map<String, Object> validCreate(String name) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("workspaceType", "ADMIN");
        request.put("name", name);
        request.put("description", "Descrição válida para " + name);
        request.put("requester", "requester");
        request.put("acronym", "AUD");
        request.put("authorizerGroup", "AUDIT_TEAM");
        request.put("settings", "{\"feature\":true}");
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
                .header("X-Correlation-Id", "workspace-audit-request")
                .header("Authorization", "Bearer workspace-audit-it")
                .accept(ContentType.JSON);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class AuditCaptureConfiguration {

        @Bean
        CapturingAuditPublisher capturingAuditPublisher() {
            return new CapturingAuditPublisher();
        }
    }

    static final class CapturingAuditPublisher implements AuditPublisher {

        private final List<AuditEventRequest> events = new CopyOnWriteArrayList<>();

        @Override
        public void publish(AuditEventRequest event) {
            events.add(event);
        }

        List<AuditEventRequest> events() {
            return List.copyOf(events);
        }

        void clear() {
            events.clear();
        }
    }
}
