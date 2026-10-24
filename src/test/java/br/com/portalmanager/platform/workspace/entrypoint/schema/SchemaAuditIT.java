package br.com.portalmanager.platform.workspace.entrypoint.schema;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import br.com.portalmanager.platform.library.audit.outbox.AuditOutboxAppender;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class SchemaAuditIT  extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport{
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthorizationMock authorization;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void lifecycleAuditsContainFlushedVersionsForSchemasAndBindings() {
        String schema = createSchema();
        String binding = request().body(Map.of("resourceType", "FEATURE", "resourceCode", schema,
                "schemaIdentifier", schema)).post("/api/v1/schema-bindings")
                .then().statusCode(201).extract().path("identifier");
        for (String base : new String[]{"/api/v1/schemas/" + schema, "/api/v1/schema-bindings/" + binding}) {
            request().patch(base + "/inactivate").then().statusCode(200);
            request().patch(base + "/activate").then().statusCode(200);
        }
        assertEvent("SCHEMA_DEACTIVATED", schema, "INACTIVE", 1);
        assertEvent("SCHEMA_ACTIVATED", schema, "ACTIVE", 2);
        assertEvent("SCHEMA_BINDING_DEACTIVATED", binding, "INACTIVE", 1);
        assertEvent("SCHEMA_BINDING_ACTIVATED", binding, "ACTIVE", 2);
        request().patch("/api/v1/schema-bindings/" + binding + "/inactivate").then().statusCode(200);
        request().delete("/api/v1/schema-bindings/" + binding).then().statusCode(204);
        assertEvent("SCHEMA_BINDING_DELETED", binding, "INACTIVE", 3);
    }

    @Test
    void draftPurgePreservesSnapshotInAuditAndHttpRemainsNoContent() {
        String schema = createSchema();
        String version = request().get("/api/v1/schemas/" + schema + "/versions")
                .then().statusCode(200).extract().path("[0].identifier");
        request().delete("/api/v1/schemas/" + schema + "/versions/" + version)
                .then().statusCode(204);
        assertThat(jdbc.queryForObject("select count(*) from schema_versions where identifier=?",
                Integer.class, version)).isZero();
        assertThat(jdbc.queryForObject("select json_unquote(json_extract(payload,'$.identifier')) "
                + "from audit_outbox where json_unquote(json_extract(metadata,'$.eventType'))='SCHEMA_VERSION_DELETED' "
                + "and json_unquote(json_extract(metadata,'$.resourceIdentifier'))=?", String.class, version))
                .isEqualTo(version);
    }

    @Test
    void auditFailureRollsBackLifecycleAndDraftPurge() {
        String schema = createSchema();
        String version = request().get("/api/v1/schemas/" + schema + "/versions")
                .then().statusCode(200).extract().path("[0].identifier");
        String binding = request().body(Map.of("resourceType", "FEATURE", "resourceCode", schema,
                "schemaIdentifier", schema)).post("/api/v1/schema-bindings")
                .then().statusCode(201).extract().path("identifier");
        doThrow(new IllegalStateException("Controlled schema audit failure"))
                .when(auditAppender).append(any(), any());
        request().patch("/api/v1/schemas/" + schema + "/inactivate").then().statusCode(500);
        request().patch("/api/v1/schema-bindings/" + binding + "/inactivate").then().statusCode(500);
        assertThat(jdbc.queryForObject("select lifecycle_code from schema_binding where identifier=?",
                String.class, binding)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("select lifecycle_code from schema_definitions where identifier=?",
                String.class, schema)).isEqualTo("ACTIVE");
        request().delete("/api/v1/schemas/" + schema + "/versions/" + version)
                .then().statusCode(500);
        assertThat(jdbc.queryForObject("select count(*) from schema_versions where identifier=?",
                Integer.class, version)).isEqualTo(1);
    }

    private void assertEvent(String event, String identifier, String lifecycle, int version) {
        Map<String, Object> snapshot = jdbc.queryForMap(
                "select json_unquote(json_extract(payload,'$.lifecycle')) as lifecycle, "
                + "cast(json_extract(payload,'$.version') as signed) as version from audit_outbox "
                + "where json_unquote(json_extract(metadata,'$.eventType'))=? "
                + "and json_unquote(json_extract(metadata,'$.resourceIdentifier'))=?", event, identifier);
        assertThat(snapshot.get("lifecycle")).isEqualTo(lifecycle);
        assertThat(((Number) snapshot.get("version")).intValue()).isEqualTo(version);
    }

    private String createSchema() {
        return request().body(Map.of("code", "audit-" + UUID.randomUUID().toString().substring(0, 8),
                "name", "Audited contract", "definition", Map.of("type", "object")))
                .post("/api/v1/schemas").then().statusCode(201).extract().path("identifier");
    }

    private RequestSpecification request() {
        return given().port(port).header("correlation-id", "schema-audit-it")
                .header("Authorization", "Bearer schema-audit-it").contentType(ContentType.JSON);
    }
}
