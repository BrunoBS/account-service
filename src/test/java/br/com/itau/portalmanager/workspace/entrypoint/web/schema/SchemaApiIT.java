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

    @BeforeEach
    void authorizeAsOwner() {
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void shouldAdministerPlatformAndWorkspaceSchemasWithVersionPublication() {
        String typeIdentifier = authorized()
                .contentType(ContentType.JSON)
                .body(Map.of(
                        "code", "application",
                        "name", "Application",
                        "description", "Application settings schema"
                ))
                .post("/api/v1/schema-types")
                .then()
                .statusCode(201)
                .body("code", equalTo("application"))
                .extract()
                .path("identifier");

        authorized()
                .get("/api/v1/schema-types/" + typeIdentifier)
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
