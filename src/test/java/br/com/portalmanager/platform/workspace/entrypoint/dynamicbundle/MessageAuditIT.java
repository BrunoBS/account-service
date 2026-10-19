package br.com.portalmanager.platform.workspace.entrypoint.dynamicbundle;

import static io.restassured.RestAssured.given;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxAppender;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@TestPropertySource(properties = "platform.audit.enabled=true")
class MessageAuditIT {

    private static final String MICROSERVICE = "11111111-1111-1111-1111-111111111111";

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthorizationMock authorization;
    @MockitoSpyBean private AuditOutboxAppender appender;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update(
                "INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings)"
                        + " VALUES ('QUARANTINED','Quarantined',3,true,'{}')");
        jdbc.update(
                "INSERT INTO service_context_microservices"
                    + " (identifier,code,name,settings,lifecycle_code,created_at,updated_at) VALUES"
                    + " (?,'review-service','Review service','{}','ACTIVE',NOW(6),NOW(6))",
                MICROSERVICE);
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
        reset(appender);
    }

    @Test
    void auditsEveryInlineTranslatorWithItsOwnIdentifierAndContent() {
        Map<String, Object> body = message();
        body.put("translators", List.of(translator("pt-BR"), translator("en-US")));
        String identifier =
                request()
                        .contentType(ContentType.JSON)
                        .body(body)
                        .post("/api/v1/dynamic-bundles/messages")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        assertSnapshot("MESSAGE_CREATED", identifier, "ACTIVE", 0);
        List<String> translators =
                jdbc.queryForList("SELECT identifier FROM message_translators", String.class);
        assertThat(translators).hasSize(2);
        for (String translatorIdentifier : translators) {
            assertSnapshot("MESSAGE_TRANSLATOR_CREATED", translatorIdentifier, "ACTIVE", 0);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT"
                                        + " JSON_UNQUOTE(JSON_EXTRACT(payload,'$.messageIdentifier'))"
                                        + " FROM audit_outbox WHERE"
                                        + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceIdentifier'))=?",
                                    String.class,
                                    translatorIdentifier))
                    .isEqualTo(identifier);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.title')) FROM"
                                        + " audit_outbox WHERE"
                                        + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceIdentifier'))=?",
                                    String.class,
                                    translatorIdentifier))
                    .isEqualTo("Title");
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.detail')) FROM"
                                        + " audit_outbox WHERE"
                                        + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceIdentifier'))=?",
                                    String.class,
                                    translatorIdentifier))
                    .isEqualTo("Detail");
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class))
                .isEqualTo(3);
    }

    @Test
    void auditsLifecycleAndUpdatesAfterFlushingVersions() {
        String identifier =
                request()
                        .contentType(ContentType.JSON)
                        .body(message())
                        .post("/api/v1/dynamic-bundles/messages")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        String translatorIdentifier =
                request()
                        .contentType(ContentType.JSON)
                        .body(translator("pt-BR"))
                        .post("/api/v1/dynamic-bundles/messages/" + identifier + "/translators")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        for (String resource :
                List.of(
                        "/api/v1/dynamic-bundles/messages/"
                                + identifier
                                + "/translators/"
                                + translatorIdentifier,
                        "/api/v1/dynamic-bundles/messages/" + identifier)) {
            boolean child = resource.contains("translators");
            String publicIdentifier = child ? translatorIdentifier : identifier;
            String prefix = child ? "MESSAGE_TRANSLATOR_" : "MESSAGE_";
            Map<String, Object> update = new HashMap<>(child ? translator("pt-BR") : message());
            update.put("version", 0);
            request()
                    .contentType(ContentType.JSON)
                    .body(update)
                    .put(resource)
                    .then()
                    .statusCode(200)
                    .body("version", equalTo(1));
            assertSnapshot(prefix + "UPDATED", publicIdentifier, "ACTIVE", 1);
            request()
                    .patch(resource + "/inactivate")
                    .then()
                    .statusCode(200)
                    .body("version", equalTo(2));
            request()
                    .patch(resource + "/activate")
                    .then()
                    .statusCode(200)
                    .body("version", equalTo(3));
            assertSnapshot(prefix + "ACTIVATED", publicIdentifier, "ACTIVE", 3);
            request()
                    .patch(resource + "/inactivate")
                    .then()
                    .statusCode(200)
                    .body("version", equalTo(4));
            request().delete(resource).then().statusCode(204);
            assertSnapshot(prefix + "PURGED", publicIdentifier, "INACTIVE", 4);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT COUNT(*) FROM audit_outbox WHERE"
                                        + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceIdentifier'))=?",
                                    Integer.class,
                                    publicIdentifier))
                    .isEqualTo(6);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM messages", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message_translators", Integer.class))
                .isZero();
    }

    @Test
    void rollsBackInlineBatchAndAllItsEventsWhenAuditFails() {
        Map<String, Object> body = message();
        body.put("translators", List.of(translator("pt-BR"), translator("en-US")));
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(
                        invocation -> {
                            if (attempts.incrementAndGet() == 3)
                                throw new IllegalStateException(
                                        "Forced message audit failure after both translator"
                                                + " events");
                            return invocation.callRealMethod();
                        })
                .when(appender)
                .append(any(), any());
        request()
                .contentType(ContentType.JSON)
                .body(body)
                .post("/api/v1/dynamic-bundles/messages")
                .then()
                .statusCode(500);
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM messages", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message_translators", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class))
                .isZero();
    }

    @Test
    void rollsBackUpdatesAndPhysicalDeletionWhenAuditFails() {
        String identifier =
                request()
                        .contentType(ContentType.JSON)
                        .body(message())
                        .post("/api/v1/dynamic-bundles/messages")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        String translatorIdentifier =
                request()
                        .contentType(ContentType.JSON)
                        .body(translator("pt-BR"))
                        .post("/api/v1/dynamic-bundles/messages/" + identifier + "/translators")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        doThrow(new IllegalStateException("Forced message audit failure"))
                .when(appender)
                .append(any(), any());
        for (String resource :
                List.of(
                        "/api/v1/dynamic-bundles/messages/"
                                + identifier
                                + "/translators/"
                                + translatorIdentifier,
                        "/api/v1/dynamic-bundles/messages/" + identifier)) {
            boolean child = resource.contains("translators");
            String table = child ? "message_translators" : "messages";
            String publicIdentifier = child ? translatorIdentifier : identifier;
            Map<String, Object> update = new HashMap<>(child ? translator("pt-BR") : message());
            update.put("version", 0);
            request()
                    .contentType(ContentType.JSON)
                    .body(update)
                    .put(resource)
                    .then()
                    .statusCode(500);
            request().patch(resource + "/inactivate").then().statusCode(500);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT version FROM " + table + " WHERE identifier=?",
                                    Integer.class,
                                    publicIdentifier))
                    .isZero();
            jdbc.update(
                    "UPDATE " + table + " SET lifecycle_code='INACTIVE' WHERE identifier=?",
                    publicIdentifier);
            request().delete(resource).then().statusCode(500);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT lifecycle_code FROM " + table + " WHERE identifier=?",
                                    String.class,
                                    publicIdentifier))
                    .isEqualTo("INACTIVE");
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void auditsCascadedPurgesAndRollsBackAllRowsAndPartialEventsOnFailure() {
        Map<String, Object> body = message();
        body.put("translators", List.of(translator("pt-BR"), translator("en-US")));
        String identifier =
                request()
                        .contentType(ContentType.JSON)
                        .body(body)
                        .post("/api/v1/dynamic-bundles/messages")
                        .then()
                        .statusCode(201)
                        .extract()
                        .path("identifier");
        String resource = "/api/v1/dynamic-bundles/messages/" + identifier;
        List<String> children =
                jdbc.queryForList(
                        "SELECT identifier FROM message_translators ORDER BY id", String.class);
        request()
                .patch(resource + "/translators/" + children.get(1) + "/inactivate")
                .then()
                .statusCode(200);
        request().patch(resource + "/inactivate").then().statusCode(200);
        int existingEvents =
                jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class);
        AtomicInteger attempts = new AtomicInteger();
        doAnswer(
                        invocation -> {
                            if (attempts.incrementAndGet() == 3)
                                throw new IllegalStateException(
                                        "Forced purge audit failure after child events");
                            return invocation.callRealMethod();
                        })
                .when(appender)
                .append(any(), any());
        request().delete(resource).then().statusCode(500);
        assertThat(attempts.get()).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM messages", Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbc.queryForList(
                                "SELECT lifecycle_code FROM message_translators ORDER BY id",
                                String.class))
                .containsExactly("ACTIVE", "INACTIVE");
        assertThat(
                        jdbc.queryForList(
                                "SELECT version FROM message_translators ORDER BY id", Long.class))
                .containsExactly(0L, 1L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class))
                .isEqualTo(existingEvents);
        reset(appender);
        request().delete(resource).then().statusCode(204);
        assertSnapshot("MESSAGE_PURGED", identifier, "INACTIVE", 1);
        assertSnapshot("MESSAGE_TRANSLATOR_PURGED", children.get(0), "ACTIVE", 0);
        assertSnapshot("MESSAGE_TRANSLATOR_PURGED", children.get(1), "INACTIVE", 1);
        for (String child : children) {
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT"
                                        + " JSON_UNQUOTE(JSON_EXTRACT(payload,'$.messageIdentifier'))"
                                        + " FROM audit_outbox WHERE"
                                        + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))='MESSAGE_TRANSLATOR_PURGED'"
                                        + " AND JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceIdentifier'))=?",
                                    String.class,
                                    child))
                    .isEqualTo(identifier);
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.title')) FROM"
                                        + " audit_outbox WHERE"
                                        + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))='MESSAGE_TRANSLATOR_PURGED'"
                                        + " AND JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceIdentifier'))=?",
                                    String.class,
                                    child))
                    .isEqualTo("Title");
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class))
                .isEqualTo(existingEvents + 3);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM messages", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM message_translators", Integer.class))
                .isZero();
    }

    private void assertSnapshot(String event, String identifier, String lifecycle, int version) {
        String predicate =
                "JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))=? AND"
                        + " JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceIdentifier'))=?";
        assertThat(
                        jdbc.queryForObject(
                                "SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.lifecycle')) FROM"
                                        + " audit_outbox WHERE "
                                        + predicate,
                                String.class,
                                event,
                                identifier))
                .isEqualTo(lifecycle);
        assertThat(
                        jdbc.queryForObject(
                                "SELECT JSON_EXTRACT(payload,'$.version') FROM audit_outbox WHERE "
                                        + predicate,
                                Integer.class,
                                event,
                                identifier))
                .isEqualTo(version);
    }

    private Map<String, Object> message() {
        return new HashMap<>(
                Map.of(
                        "microserviceIdentifier",
                        MICROSERVICE,
                        "messageKey",
                        "review.audit",
                        "code",
                        "REVIEW-101",
                        "httpStatus",
                        400));
    }

    private Map<String, Object> translator(String locale) {
        return Map.of(
                "locale", locale, "title", "Title", "detail", "Detail", "suggestion", "Suggestion");
    }

    private RequestSpecification request() {
        return given().port(port)
                .header("correlation-id", "message-audit-it")
                .header("Authorization", "Bearer message-audit-it")
                .accept(ContentType.JSON);
    }
}
