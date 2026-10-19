package br.com.portalmanager.platform.workspace.entrypoint.environment;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
@TestPropertySource(properties = "platform.audit.enabled=true")
class EnvironmentTypePurgeIT {
    private static final String TYPE_CODE = "CELL";

    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthorizationMock authorization;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean
    private br.com.portalmanager.platform.library.audit.outbox.AuditOutboxAppender auditOutboxAppender;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        for (String code : new String[] { "ACTIVE", "INACTIVE" }) {
            jdbc.update("INSERT IGNORE INTO type_life_cycle (code,label,description,sort_order,is_active,settings) VALUES (?,?,?,1,true,'{}')", code, code, code);
        }
        jdbc.update("DELETE FROM environment_type_compatibilities WHERE child_type_code=? OR parent_type_code=?", TYPE_CODE, TYPE_CODE);
        jdbc.update("DELETE FROM environments WHERE environment_type_code=?", TYPE_CODE);
        jdbc.update("DELETE FROM environment_types WHERE code=?", TYPE_CODE);
        jdbc.update("INSERT INTO environment_types (version,code,name,description,root_allowed,allows_children,workspace_required,lifecycle_code,display_order,created_at,updated_at) VALUES (7,?,'Purge snapshot','Persisted details',false,false,true,'INACTIVE',4,NOW(6),NOW(6))", TYPE_CODE);
        jdbc.update("INSERT IGNORE INTO environment_types (code,name,description,root_allowed,allows_children,workspace_required,lifecycle_code,display_order,created_at,updated_at) VALUES ('SHARD','Shard','Parent',false,true,true,'ACTIVE',3,NOW(6),NOW(6))");
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void capturesThePersistedSnapshotBeforePhysicallyPurgingTheType() {
        delete().then().statusCode(204);
        assertThat(typeCount()).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType')) FROM audit_outbox", String.class)).isEqualTo("ENVIRONMENT_TYPE_PURGED");
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.resourceIdentifier')) FROM audit_outbox", String.class)).isEqualTo(TYPE_CODE);
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.lifecycle')) FROM audit_outbox", String.class)).isEqualTo("INACTIVE");
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.version')) FROM audit_outbox", String.class)).isEqualTo("7");
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.description')) FROM audit_outbox", String.class)).isEqualTo("Persisted details");
    }

    @Test
    void rejectedPurgesDoNotPublishAnEventOrRemoveTheType() {
        jdbc.update("UPDATE environment_types SET lifecycle_code='ACTIVE' WHERE code=?", TYPE_CODE);
        delete().then().statusCode(400);
        given().port(port).header("correlation-id", "environment-type-purge-it").header("Authorization", "Bearer environment-type-purge-it")
            .delete("/api/v1/environment-types/DOES_NOT_EXIST").then().statusCode(404);
        jdbc.update("UPDATE environment_types SET lifecycle_code='INACTIVE' WHERE code=?", TYPE_CODE);
        jdbc.update("INSERT INTO environment_type_compatibilities (identifier,parent_type_code,child_type_code,lifecycle_code,created_at,updated_at) VALUES (UUID(),'SHARD',?,'ACTIVE',NOW(6),NOW(6))", TYPE_CODE);
        delete().then().statusCode(400);
        assertThat(typeCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isZero();
    }

    @Test
    void rollsBackThePurgeWhenTheAuditOutboxCannotBeWritten() {
        org.mockito.Mockito.doThrow(new IllegalStateException("Forced audit failure"))
            .when(auditOutboxAppender).append(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        delete().then().statusCode(500);
        assertThat(typeCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isZero();
    }

    @Test
    void restorationReturnsAndAuditsTheFlushedVersion() {
        given().port(port).header("correlation-id", "environment-type-purge-it")
            .header("Authorization", "Bearer environment-type-purge-it")
            .post("/api/v1/environment-types/" + TYPE_CODE + "/restore").then()
            .statusCode(200).body("version", org.hamcrest.Matchers.equalTo(8))
            .body("lifecycle", org.hamcrest.Matchers.equalTo("ACTIVE"));
        assertThat(jdbc.queryForObject("SELECT version FROM environment_types WHERE code=?", Long.class, TYPE_CODE)).isEqualTo(8L);
        assertThat(jdbc.queryForObject("SELECT JSON_UNQUOTE(JSON_EXTRACT(payload,'$.version')) FROM audit_outbox WHERE JSON_UNQUOTE(JSON_EXTRACT(metadata,'$.eventType'))='ENVIRONMENT_TYPE_RESTORED'", String.class)).isEqualTo("8");
        given().port(port).header("correlation-id", "environment-type-purge-it")
            .header("Authorization", "Bearer environment-type-purge-it").contentType(io.restassured.http.ContentType.JSON)
            .body(Map.of("version", 8, "code", TYPE_CODE, "name", "Restored type", "description", "Edited after restore",
                "rootAllowed", false, "allowsChildren", false, "workspaceRequired", true, "displayOrder", 4))
            .put("/api/v1/environment-types/" + TYPE_CODE).then().statusCode(200)
            .body("version", org.hamcrest.Matchers.equalTo(9));
    }

    @Test
    void rejectsPurgeWhenAnEnvironmentStillReferencesTheType() {
        jdbc.update("INSERT IGNORE INTO type_authorizations (code,label,description,sort_order,is_active,settings) VALUES ('DEV','Dev','Developer',1,true,'{}')");
        jdbc.update("INSERT INTO environments (identifier,environment_type_code,name,description,authorization_type_code,settings,sort_order,lifecycle_code,created_at,updated_at) VALUES (UUID(),?,'Existing environment','Historical reference','DEV','{}',1,'INACTIVE',NOW(6),NOW(6))", TYPE_CODE);
        delete().then().statusCode(400).body("details.field", org.hamcrest.Matchers.hasItem("code"));
        assertThat(typeCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM audit_outbox", Integer.class)).isZero();
    }

    private Integer typeCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM environment_types WHERE code=?", Integer.class, TYPE_CODE);
    }

    private io.restassured.response.Response delete() {
        return given().port(port).header("correlation-id", "environment-type-purge-it")
            .header("Authorization", "Bearer environment-type-purge-it")
            .delete("/api/v1/environment-types/" + TYPE_CODE);
    }
}
