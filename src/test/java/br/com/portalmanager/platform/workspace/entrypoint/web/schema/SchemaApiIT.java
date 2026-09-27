package br.com.portalmanager.platform.workspace.entrypoint.web.schema;

import br.com.portalmanager.platform.library.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class SchemaApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthorizationMock authorizationMock;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void authorizeAsOwner() {
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("PM5_OWNER"));

        jdbc.update("""
                insert into type_life_cycle (code, label, description, sort_order, is_active, settings)
                values
                    ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}'),
                    ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}'),
                    ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}')
                on duplicate key update code = values(code)
                """);

        jdbc.update("""
                insert into type_schema_scopes (code, label, description, sort_order, is_active, settings)
                values
                    ('PLATFORM', 'Platform', 'Platform-owned schema', 1, true, '{}'),
                    ('WORKSPACE', 'Workspace', 'Workspace-owned schema', 2, true, '{}')
                on duplicate key update code = values(code)
                """);

        jdbc.update("""
                insert into type_schema_version_status (code, label, description, sort_order, is_active, settings)
                values
                    ('DRAFT', 'Draft', 'Schema version under edition', 1, true, '{}'),
                    ('PUBLISHED', 'Published', 'Published immutable schema version', 2, true, '{}')
                on duplicate key update code = values(code)
                """);
    }

    @Test
    void shouldAdministerPlatformAndWorkspaceSchemasWithVersionPublication() {
        String schemaTypeIdentifier = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "code", "APPLICATION",
                        "name", "Application",
                        "description", "Application settings schema",
                        "scope", "PLATFORM"
                ))
                .post("/api/v1/schema-types")
                .then()
                .statusCode(201)
                .body("code", equalTo("APPLICATION"))
                .body("name", equalTo("Application"))
                .body("scope", equalTo("PLATFORM"))
                .extract()
                .path("identifier");

        authorized()
                .get("/api/v1/schema-types/" + schemaTypeIdentifier)
                .then()
                .statusCode(200)
                .body("code", equalTo("APPLICATION"));

        String platformSchemaIdentifier = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "schemaTypeCode", "application",
                        "code", "application",
                        "name", "Application Platform Schema",
                        "description", "Platform schema for application settings",
                        "definition", Map.of("type", "object")
                ))
                .post("/api/v1/schemas")
                .then()
                .statusCode(201)
                .body("schemaTypeCode", equalTo("APPLICATION"))
                .body("scope", equalTo("PLATFORM"))
                .extract()
                .path("identifier");

        authorized()
                .get("/api/v1/schemas/" + UUID.randomUUID())
                .then()
                .statusCode(404);

        authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "schemaTypeCode", "APPLICATION",
                        "code", "INVALID_CODE",
                        "name", "Invalid Schema",
                        "definition", Map.of("type", "object")
                ))
                .post("/api/v1/schemas")
                .then()
                .statusCode(400);

        authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "schemaTypeCode", "APPLICATION",
                        "code", "application",
                        "name", "Duplicate Schema",
                        "definition", Map.of("type", "object")
                ))
                .post("/api/v1/schemas")
                .then()
                .statusCode(409);

        authorized()
                .contentType(ContentType.JSON)
                .body(Map.of("version", -1, "name", "Updated schema"))
                .put("/api/v1/schemas/" + platformSchemaIdentifier)
                .then()
                .statusCode(409);

        authorized()
                .get("/api/v1/schemas/" + platformSchemaIdentifier + "/versions")
                .then()
                .statusCode(200)
                .body("version", hasItem(1))
                .body("status", hasItem("DRAFT"));

        String versionIdentifier = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "versionName", "v1",
                        "definition", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "repositoryUrl", Map.of("type", "string")
                                )
                        )
                ))
                .post("/api/v1/schemas/" + platformSchemaIdentifier + "/versions")
                .then()
                .statusCode(201)
                .body("version", equalTo(1))
                .body("status", equalTo("DRAFT"))
                .extract()
                .path("identifier");

        authorized()
                .patch("/api/v1/schemas/" + platformSchemaIdentifier
                        + "/versions/" + versionIdentifier + "/publish")
                .then()
                .statusCode(200)
                .body("status", equalTo("PUBLISHED"));

        authorized()
                .delete("/api/v1/schemas/" + platformSchemaIdentifier
                        + "/versions/" + versionIdentifier)
                .then()
                .statusCode(409);

        authorized()
                .patch("/api/v1/schemas/" + platformSchemaIdentifier
                        + "/versions/" + UUID.randomUUID() + "/publish")
                .then()
                .statusCode(404);

        authorized()
                .get("/api/v1/schemas/" + platformSchemaIdentifier + "/versions")
                .then()
                .statusCode(200)
                .body("version", hasItem(1))
                .body("status", hasItem("PUBLISHED"));

        String workspaceIdentifier = UUID.randomUUID().toString();
        seedWorkspace(workspaceIdentifier);

        authorized()
                .contentType(ContentType.JSON)
                .body(Map.of("code", "WORKSPACE_APPLICATION", "name", "Workspace application",
                        "scope", "WORKSPACE"))
                .post("/api/v1/schema-types")
                .then()
                .statusCode(201);

        String workspaceSchemaIdentifier = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "schemaTypeCode", "WORKSPACE_APPLICATION",
                        "code", "custom-application",
                        "name", "Workspace Application Schema",
                        "description", "Workspace-owned application schema",
                        "definition", Map.of("type", "object")
                ))
                .post("/api/v1/workspaces/" + workspaceIdentifier + "/schemas")
                .then()
                .statusCode(201)
                .body("scope", equalTo("WORKSPACE"))
                .body("workspaceIdentifier", equalTo(workspaceIdentifier))
                .extract()
                .path("identifier");

        authorized()
                .get("/api/v1/workspaces/" + workspaceIdentifier + "/schemas")
                .then()
                .statusCode(200)
                .body("identifier", hasItem(workspaceSchemaIdentifier));

        authorized()
                .get("/api/v1/schemas")
                .then()
                .statusCode(200)
                .body("identifier", hasItem(platformSchemaIdentifier));
    }


    @Test
    void shouldEnforceSchemaTypeScopeAndDeletionRules() {
        String workspaceIdentifier = UUID.randomUUID().toString();
        seedWorkspace(workspaceIdentifier);

        Map<String, Object> platformOnlyType = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "code", "PLATFORM_ONLY",
                        "name", "Platform only",
                        "description", "Platform-only schema type",
                        "scope", "PLATFORM"
                ))
                .post("/api/v1/schema-types")
                .then()
                .statusCode(201)
                .extract()
                .as(Map.class);

        String platformOnlyIdentifier = (String) platformOnlyType.get("identifier");
        Number platformOnlyVersion = (Number) platformOnlyType.get("version");

        authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "schemaTypeCode", "PLATFORM_ONLY",
                        "code", "workspace-forbidden",
                        "name", "Workspace forbidden",
                        "definition", Map.of("type", "object")
                ))
                .post("/api/v1/workspaces/" + workspaceIdentifier + "/schemas")
                .then()
                .statusCode(400);

        authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "schemaTypeCode", "PLATFORM_ONLY",
                        "code", "platform-only",
                        "name", "Platform only schema",
                        "definition", Map.of("type", "object")
                ))
                .post("/api/v1/schemas")
                .then()
                .statusCode(201);

        authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "version", platformOnlyVersion.longValue(),
                        "name", "Platform only",
                        "description", "Attempt to remove used platform scope",
                        "scope", "WORKSPACE"
                ))
                .put("/api/v1/schema-types/" + platformOnlyIdentifier)
                .then()
                .statusCode(409);

        authorized()
                .delete("/api/v1/schema-types/" + platformOnlyIdentifier)
                .then()
                .statusCode(409);

        authorized()
                .patch("/api/v1/schema-types/" + platformOnlyIdentifier + "/inactivate")
                .then()
                .statusCode(200);

        authorized()
                .delete("/api/v1/schema-types/" + platformOnlyIdentifier)
                .then()
                .statusCode(409);

        String unusedTypeIdentifier = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "code", "UNUSED",
                        "name", "Unused",
                        "scope", "PLATFORM"
                ))
                .post("/api/v1/schema-types")
                .then()
                .statusCode(201)
                .extract()
                .path("identifier");

        authorized()
                .delete("/api/v1/schema-types/" + unusedTypeIdentifier)
                .then()
                .statusCode(409);

        authorized()
                .patch("/api/v1/schema-types/" + unusedTypeIdentifier + "/inactivate")
                .then()
                .statusCode(200);

        authorized()
                .delete("/api/v1/schema-types/" + unusedTypeIdentifier)
                .then()
                .statusCode(204);
    }

    @Test
    void shouldProtectDefaultSchemaTypeAndDefaultPlatformSchema() {
        seedDefaultFallback();
        String defaultTypeIdentifier = jdbc.queryForObject(
                "select identifier from schema_types where code = 'DEFAULT'",
                String.class
        );
        String defaultSchemaIdentifier = jdbc.queryForObject(
                "select identifier from schema_definitions " +
                        "where schema_type_code = 'DEFAULT' and scope_code = 'PLATFORM'",
                String.class
        );

        authorized()
                .patch("/api/v1/schema-types/" + defaultTypeIdentifier + "/inactivate")
                .then()
                .statusCode(409);

        authorized()
                .delete("/api/v1/schema-types/" + defaultTypeIdentifier)
                .then()
                .statusCode(409);

        authorized()
                .patch("/api/v1/schemas/" + defaultSchemaIdentifier + "/inactivate")
                .then()
                .statusCode(409);

        authorized()
                .delete("/api/v1/schemas/" + defaultSchemaIdentifier)
                .then()
                .statusCode(409);
    }

    private void seedDefaultFallback() {
        jdbc.update("""
                insert into schema_types
                    (version, identifier, code, name, description, lifecycle_code, scope_code, created_at, updated_at)
                values
                    (0, UUID(), 'DEFAULT', 'Default', 'Platform fallback schema type',
                     'ACTIVE', 'PLATFORM', current_timestamp, current_timestamp)
                """);

        jdbc.update("""
                insert into schema_definitions
                    (version, identifier, schema_type_code, scope_code, workspace_id,
                     code, name, description, lifecycle_code, created_at, updated_at)
                values
                    (0, UUID(), 'DEFAULT', 'PLATFORM', null,
                     'default', 'Default', 'Permissive platform fallback',
                     'ACTIVE', current_timestamp, current_timestamp)
                """);
    }

    private void seedWorkspace(String identifier) {
        jdbc.update("""
                insert into type_workspaces (code, label, description, sort_order, is_active, settings)
                values ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}')
                on duplicate key update code = values(code)
                """);

        jdbc.update("""
                        insert into workspaces
                            (version, identifier, workspace_type_code, name, description, requester,
                             acronym, settings, authorizer_group, email_group, onboarding,
                             lifecycle_code, created_at, updated_at)
                        values
                            (0, ?, 'ADMIN', ?, 'Schema integration workspace', 'integration-test',
                             'SCH', null, null, 'schema-it@example.com', false,
                             'ACTIVE', current_timestamp, current_timestamp)
                        """,
                identifier,
                "Schema IT " + identifier.substring(0, 8)
        );
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
                .port(port)
                .header("X-Correlation-Id", "schema-api-it")
                .header("Authorization", "Bearer schema-api-it")
                .accept(ContentType.JSON);
    }
}
