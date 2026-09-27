package br.com.portalmanager.platform.workspace.entrypoint.web.publisher;

import br.com.portalmanager.platform.library.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaResolutionPort;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.Map;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static io.restassured.RestAssured.given;
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
                INSERT IGNORE INTO type_schema_scopes (code, label, description, sort_order, is_active, settings)
                VALUES ('PLATFORM', 'Platform', 'Schema owned by the platform', 1, true, '{}')
                """);
        jdbc.update("""
                INSERT IGNORE INTO type_resource_scopes (code, label, description, sort_order, is_active, settings)
                VALUES ('WORKSPACE', 'Workspace', 'Workspace resources', 1, true, '{}'),
                       ('APPLICATION', 'Application', 'Application resources', 2, true, '{}')
                """);
        for (String code : new String[] {"WEB_SOCKET", "KAAS", "APPCONFIG"}) {
            jdbc.update("""
                    INSERT IGNORE INTO schema_types
                      (version, identifier, code, name, description, lifecycle_code, scope_code, created_at, updated_at)
                    VALUES (0, ?, ?, ?, 'Platform publisher schema type', 'ACTIVE', 'PLATFORM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, UUID.randomUUID().toString(), "PUBLISHER_" + code, code);
        }
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void createsPublisherWithSeededTypeAndKeepsSchemaOutsidePublisher() {
        String identifier = post(input("WEB_SOCKET", "Web Socket", "WORKSPACE")).statusCode(201)
                .body("schemaTypeCode", equalTo("PUBLISHER_WEB_SOCKET"))
                .body("code", equalTo("WEB_SOCKET"))
                .body("lifecycle", equalTo("ACTIVE")).extract().path("identifier");
        get("/api/v1/publishers/" + identifier).statusCode(200).body("code", equalTo("WEB_SOCKET"));
        get("/api/v1/publishers?scope=APPLICATION").statusCode(200).body("identifier", not(hasItem(identifier)));
        post(input("WEB_SOCKET", "Duplicate", "WORKSPACE")).statusCode(400).body("details.field", hasItem("code"));
        post("/api/v1/publishers/" + identifier + "/inactivate").statusCode(204);
        get("/api/v1/publishers/" + identifier).statusCode(404);
        post("/api/v1/publishers/" + identifier + "/restore").statusCode(200).body("lifecycle", equalTo("ACTIVE"));
    }

    @Test
    void acceptsNewCodeAfterPlatformSchemaTypeRegistrationWithoutChangingEnum() {
        String code = "NEW_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        post(input(code, "New Publisher", "APPLICATION")).statusCode(400)
                .body("details.field", hasItem("code"));
        jdbc.update("""
                INSERT INTO schema_types
                  (version, identifier, code, name, description, lifecycle_code, scope_code, created_at, updated_at)
                VALUES (0, ?, ?, 'New publisher schema type', 'Dynamic publisher', 'ACTIVE', 'PLATFORM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, UUID.randomUUID().toString(), "PUBLISHER_" + code);
        String identifier = post(input(code, "New Publisher", "APPLICATION")).statusCode(201)
                .body("schemaTypeCode", equalTo("PUBLISHER_" + code)).extract().path("identifier");
        get("/api/v1/publishers?scope=APPLICATION").statusCode(200).body("identifier", hasItem(identifier));
    }

    @Test
    void requiresMatchingActivePlatformSchemaTypeAndActiveResourceScope() {
        post(input("KAAS", "Kaas", "WORKSPACE")).statusCode(201)
                .body("schemaTypeCode", equalTo("PUBLISHER_KAAS"));
        post(input("APPCONFIG", "AppConfig", "APPLICATION")).statusCode(201)
                .body("schemaTypeCode", equalTo("PUBLISHER_APPCONFIG"));

        jdbc.update("UPDATE schema_types SET lifecycle_code = 'INACTIVE' WHERE code = 'PUBLISHER_WEB_SOCKET'");
        post(input("WEB_SOCKET", "Web Socket", "WORKSPACE")).statusCode(400)
                .body("details.field", hasItem("schemaTypeCode"));

        jdbc.update("UPDATE type_resource_scopes SET is_active = false WHERE code = 'APPLICATION'");
        post(input("WEB_SOCKET", "Web Socket", "APPLICATION")).statusCode(400)
                .body("details.field", hasItem("scope"));
    }

    @Test
    void resolvesDifferentPublishedSchemasForDifferentPublisherCodes() {
        String webSocketDefinition = "{\"type\":\"object\",\"required\":[\"url\"]}";
        String kaasDefinition = "{\"type\":\"object\",\"required\":[\"bucket\"]}";
        registerPublishedSchema("PUBLISHER_WEB_SOCKET", "web-socket", webSocketDefinition);
        registerPublishedSchema("PUBLISHER_KAAS", "kaas", kaasDefinition);

        post(input("WEB_SOCKET", "Web Socket", "WORKSPACE")).statusCode(201)
                .body("schemaTypeCode", equalTo("PUBLISHER_WEB_SOCKET"));
        post(input("KAAS", "Kaas", "WORKSPACE")).statusCode(201)
                .body("schemaTypeCode", equalTo("PUBLISHER_KAAS"));

        assertThat(schemas.resolvePlatform("PUBLISHER_WEB_SOCKET")).isEqualTo(webSocketDefinition);
        assertThat(schemas.resolvePlatform("PUBLISHER_KAAS")).isEqualTo(kaasDefinition);
    }

    private void registerPublishedSchema(String schemaType, String code, String definition) {
        jdbc.update("""
                INSERT INTO schema_definitions
                  (version, identifier, schema_type_code, scope_code, code, name, lifecycle_code, created_at, updated_at)
                VALUES (0, ?, ?, 'PLATFORM', ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, UUID.randomUUID().toString(), schemaType, code, code);
        Long schemaId = jdbc.queryForObject(
                "SELECT id FROM schema_definitions WHERE schema_type_code = ?", Long.class, schemaType);
        jdbc.update("""
                INSERT INTO schema_versions
                  (identifier, schema_id, schema_version, version_name, definition, status, created_at)
                VALUES (?, ?, 1, 'v1', ?, 'PUBLISHED', CURRENT_TIMESTAMP)
                """, UUID.randomUUID().toString(), schemaId, definition);
    }

    private Map<String, Object> input(String code, String name, String scope) {
        return Map.of("code", code, "name", name, "description", "Publisher for integration", "scope", scope);
    }
    private ValidatableResponse post(Object body) {
        return given().port(port).header("X-Correlation-Id", "publisher-it")
                .header("Authorization", "Bearer publisher-it").contentType(ContentType.JSON)
                .body(body).when().post("/api/v1/publishers").then();
    }
    private ValidatableResponse post(String path) {
        return given().port(port).header("X-Correlation-Id", "publisher-it")
                .header("Authorization", "Bearer publisher-it").when().post(path).then();
    }
    private ValidatableResponse get(String path) {
        return given().port(port).header("X-Correlation-Id", "publisher-it")
                .header("Authorization", "Bearer publisher-it").accept(ContentType.JSON).when().get(path).then();
    }
}
