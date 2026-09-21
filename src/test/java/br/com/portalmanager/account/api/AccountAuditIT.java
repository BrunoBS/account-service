package br.com.portalmanager.account.api;

import br.com.portalmanager.core.audit.model.AuditEventRequest;
import br.com.portalmanager.core.audit.publisher.AuditPublisher;
import br.com.portalmanager.core.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.core.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.core.testing.annotation.WithMySql;
import br.com.portalmanager.core.testing.authorization.AuthorizationMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
@Import(AccountAuditIT.AuditCaptureConfiguration.class)
class AccountAuditIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthorizationMock authorizationMock;

    @Autowired
    private CapturingAuditPublisher auditPublisher;

    @DynamicPropertySource
    static void auditProperties(DynamicPropertyRegistry registry) {
        registry.add("platform.audit.enabled", () -> true);
        registry.add("platform.audit.service-name", () -> "account-service");
    }

    @BeforeEach
    void setUp() {
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
    void shouldAuditAccountMutations() {
        var created = post(validCreate("Conta Audit"))
                .statusCode(201)
                .extract();

        Integer id = created.path("id");
        Integer version = created.path("version");

        Map<String, Object> update = validCreate("Conta Audit");
        update.put("version", version);
        update.put("description", "Descrição atualizada para auditoria");

        put(id, update).statusCode(200);
        delete(id).statusCode(204);
        post("/api/v1/accounts/" + id + "/restore").statusCode(200);

        List<AuditEventRequest> events = new ArrayList<>(auditPublisher.events());

        assertThat(events).hasSize(4);
        assertThat(events)
                .extracting(AuditEventRequest::resource)
                .containsOnly("ACCOUNT");
        assertThat(events)
                .extracting(AuditEventRequest::action)
                .containsExactly("INSERT", "UPDATE", "DELETE", "RESTORE");
        assertThat(events)
                .extracting(AuditEventRequest::resourceId)
                .containsOnly(String.valueOf(id));
        assertThat(events)
                .extracting(AuditEventRequest::service)
                .containsOnly("account-service");
        assertThat(events)
                .extracting(AuditEventRequest::actor)
                .containsOnly("golden-auditor");
        assertThat(events)
                .extracting(AuditEventRequest::correlationId)
                .containsOnly("audit-trace");
    }

    private Map<String, Object> validCreate(String name) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("accountType", "ADMIN");
        request.put("name", name);
        request.put("description", "Descrição válida para " + name);
        request.put("requester", "requester");
        request.put("acronym", "AUD");
        request.put("authorizerGroup", "AUDIT_TEAM");
        request.put("settings", "{\"feature\":true}");
        request.put("emailGroup", "account@portalmanager.com");
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
                .post("/api/v1/accounts")
                .then();
    }

    private io.restassured.response.ValidatableResponse post(String path) {
        return authorized().when().post(path).then();
    }

    private io.restassured.response.ValidatableResponse put(Integer id, Map<String, Object> body) {
        return authorized()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put("/api/v1/accounts/" + id)
                .then();
    }

    private io.restassured.response.ValidatableResponse delete(Integer id) {
        return authorized().when().delete("/api/v1/accounts/" + id).then();
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
                .port(port)
                .header("X-Correlation-Id", "audit-request")
                .header("Authorization", "Bearer audit-it")
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
