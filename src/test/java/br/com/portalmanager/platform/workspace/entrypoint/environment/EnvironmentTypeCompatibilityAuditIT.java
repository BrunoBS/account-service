package br.com.portalmanager.platform.workspace.entrypoint.environment;

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
import io.restassured.response.ValidatableResponse;
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
class EnvironmentTypeCompatibilityAuditIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuthorizationMock authorization;

    @MockitoSpyBean
    private AuditOutboxAppender auditAppender;

    private static final String BASE = "/api/v1/environment-types/compatibilities";

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update("INSERT IGNORE INTO environment_types (code,name,description,root_allowed,allows_children,workspace_required,lifecycle_code,display_order,created_at,updated_at) VALUES ('DEFAULT','Parent','Parent',true,true,false,'ACTIVE',1,NOW(6),NOW(6)),('SHARD','Child','Child',false,true,true,'ACTIVE',2,NOW(6),NOW(6))");
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void auditsInactivationAndPurgeWithTheCorrectCompatibilitySnapshot() {
        String identifier = create();
        post(BASE + "/" + identifier + "/inactivate", null).statusCode(204);
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.lifecycle')) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))='ENVIRONMENT_TYPE_COMPATIBILITY_DISALLOWED'", String.class)).isEqualTo("INACTIVE");
        given().port(port).header("correlation-id", "compatibility-audit-it").header("Authorization", "Bearer compatibility-audit-it")
            .delete(BASE + "/" + identifier).then().statusCode(204);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM environment_type_compatibilities WHERE identifier=?", Integer.class, identifier)).isZero();
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.identifier')) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))='ENVIRONMENT_TYPE_COMPATIBILITY_PURGED'", String.class)).isEqualTo(identifier);
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.parentTypeCode')) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))='ENVIRONMENT_TYPE_COMPATIBILITY_PURGED'", String.class)).isEqualTo("DEFAULT");
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.lifecycle')) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))='ENVIRONMENT_TYPE_COMPATIBILITY_PURGED'", String.class)).isEqualTo("INACTIVE");
    }

    @Test
    void rejectedPurgesPublishNoEvent() {
        String identifier = create();
        given().port(port).header("correlation-id", "compatibility-audit-it").header("Authorization", "Bearer compatibility-audit-it")
            .delete(BASE + "/" + identifier).then().statusCode(400);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM environment_type_compatibilities WHERE identifier=?", Integer.class, identifier)).isEqualTo(1);
    }

    @Test
    void rollsBackInactivationAndPurgeWhenAuditFails() {
        String identifier = create();
        doThrow(new IllegalStateException("Forced compatibility audit failure")).when(auditAppender).append(any(), any());
        post(BASE + "/" + identifier + "/inactivate", null).statusCode(500);
        assertThat(jdbc.queryForObject("SELECT lifecycle_code FROM environment_type_compatibilities WHERE identifier=?", String.class, identifier)).isEqualTo("ACTIVE");
        jdbc.update("UPDATE environment_type_compatibilities SET lifecycle_code='INACTIVE' WHERE identifier=?", identifier);
        given().port(port).header("correlation-id", "compatibility-audit-it").header("Authorization", "Bearer compatibility-audit-it")
            .delete(BASE + "/" + identifier).then().statusCode(500);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM environment_type_compatibilities WHERE identifier=?", Integer.class, identifier)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isEqualTo(1);
    }

    private String create() {
        return post(BASE, Map.of("parentTypeCode", "DEFAULT", "childTypeCode", "SHARD"))
            .statusCode(201).extract().path("identifier");
    }

    private ValidatableResponse post(String path, Object body) {
        io.restassured.specification.RequestSpecification request = given().port(port)
            .header("correlation-id", "compatibility-audit-it").header("Authorization", "Bearer compatibility-audit-it");
        if (body != null) request.contentType(ContentType.JSON).body(body);
        return request.post(path).then();
    }
}
