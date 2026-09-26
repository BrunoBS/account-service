package br.com.itau.portalmanager.workspace.entrypoint.web.schema;

import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
import br.com.portalmanager.platform.testing.authorization.AuthorizationMock;
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
    }

    @Test
    void shouldAdministerPlatformAndWorkspaceSchemasWithVersionPublication() {
        authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "code", "application",
                        "label", "Application",
                        "description", "Application settings schema",
                        "sortOrder", 10,
                        "settings", Map.of()
                ))
                .post("/api/v1/schema-type")
                .then()
                .statusCode(201)
                .body("code", equalTo("application"))
                .body("label", equalTo("Application"));

        authorized()
                .get("/api/v1/schema-type/application")
                .then()
                .statusCode(200)
                .body("code", equalTo("application"));

        String platformSchemaIdentifier = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "schemaTypeCode", "application",
                        "code", "application",
                        "name", "Application Platform Schema",
                        "description", "Platform schema for application settings"
                ))
                .post("/api/v1/schemas")
                .then()
                .statusCode(201)
                .body("schemaTypeCode", equalTo("application"))
                .body("scope", equalTo("PLATFORM"))
                .extract()
                .path("identifier");

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
                .get("/api/v1/schemas/" + platformSchemaIdentifier + "/versions")
                .then()
                .statusCode(200)
                .body("version", hasItem(1))
                .body("status", hasItem("PUBLISHED"));

        String workspaceIdentifier = UUID.randomUUID().toString();

        String workspaceSchemaIdentifier = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "schemaTypeCode", "application",
                        "code", "custom-application",
                        "name", "Workspace Application Schema",
                        "description", "Workspace-owned application schema"
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

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
                .port(port)
                .header("X-Correlation-Id", "schema-api-it")
                .header("Authorization", "Bearer schema-api-it")
                .accept(ContentType.JSON);
    }
}
