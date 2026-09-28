package br.com.portalmanager.platform.workspace.entrypoint.web.publisher;

import br.com.portalmanager.platform.library.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.foundation.schema.domain.SchemaResourceType;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaResolutionPort;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class PublisherApiIT {
    @LocalServerPort private int port;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaResolutionPort schemas;

    @BeforeEach
    void prepare() {
        jdbc.update("""
                INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings)
                VALUES ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}'),
                       ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}'),
                       ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}')
                """);
        jdbc.update("""
                INSERT IGNORE INTO type_resource_scopes (code, label, description, sort_order, is_active, settings)
                VALUES ('WORKSPACE', 'Workspace', 'Workspace resources', 1, true, '{}'),
                       ('APPLICATION', 'Application', 'Application resources', 2, true, '{}')
                """);
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void createsPublisherWithoutSchemaBindingOrSettings() {
        String code = "P_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String identifier = given().port(port).header("X-Correlation-Id", "publisher-it")
                .header("Authorization", "Bearer publisher-it")
                .contentType(ContentType.JSON)
                .body(Map.of("code", code, "name", "Test Publisher",
                        "description", "Publisher for integration", "scope", "WORKSPACE"))
                .when().post("/api/v1/publishers").then()
                .statusCode(201).body("code", equalTo(code)).body("lifecycle", equalTo("ACTIVE"))
                .extract().path("identifier");
        given().port(port).header("X-Correlation-Id", "publisher-it")
                .header("Authorization", "Bearer publisher-it")
                .when().get("/api/v1/publishers/" + identifier).then().statusCode(200);
        assertThat(schemas.resolve(SchemaResourceType.PUBLISHER, code))
                .isEqualTo(jdbc.queryForObject("""
                        select v.definition from schema_configuration c
                        join schema_versions v on v.schema_id = c.schema_id
                        where c.resource_type = 'PUBLISHER' and c.resource_code = 'DEFAULT'
                          and v.status = 'PUBLISHED'
                        order by v.schema_version desc limit 1
                        """, String.class));
    }

    @Test
    void invalidPublisherScopeIsRejectedIndependentlyOfSchemaConfiguration() {
        String code = "P_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        given().port(port).header("X-Correlation-Id", "publisher-it")
                .header("Authorization", "Bearer publisher-it").contentType(ContentType.JSON)
                .body(Map.of("code", code, "name", "Test Publisher",
                        "description", "Publisher for integration", "scope", "INVALID"))
                .when().post("/api/v1/publishers").then().statusCode(400)
                .body("details.field", hasItem("scope"));
    }
}
