package br.com.portalmanager.platform.workspace.entrypoint.schema;

import io.restassured.path.json.JsonPath;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.foundation.schema.integration.SchemaResolutionPort;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class SchemaApiIT {

    @LocalServerPort
    private int port;

    @Autowired
    private AuthorizationMock authorization;

    @Autowired
    private SchemaResolutionPort resolver;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void schemaConfigurationFallsBackBeforePublicationAndResolvesPublishedDefinition() {
        String code = "contract-" + UUID.randomUUID().toString().substring(0, 8);
        String schema = post(
            "/api/v1/schemas",
            Map.of("code", code, "name", "Platform Contract", "definition", Map.of("type", "object"))
        )
            .statusCode(201)
            .body("scope", equalTo("PLATFORM"))
            .extract()
            .path("identifier");

        String binding = post(
            "/api/v1/schema-bindings",
            Map.of("resourceType", "FEATURE", "resourceCode", code, "schemaIdentifier", schema)
        )
            .statusCode(201)
            .body("resourceCode", equalTo(code))
            .extract()
            .path("identifier");
        assertThat(resolver.resolve("FEATURE", code)).isEqualTo(defaultDefinition("FEATURE"));

        String version = get("/api/v1/schemas/" + schema + "/versions")
            .statusCode(200)
            .extract()
            .path("[0].identifier");
        patch("/api/v1/schemas/" + schema + "/versions/" + version + "/publish").statusCode(200);
        assertThat(resolver.resolve("FEATURE", code)).contains("\"type\"").contains("\"object\"");

        post(
            "/api/v1/schema-bindings",
            Map.of("resourceType", "FEATURE", "resourceCode", code, "schemaIdentifier", schema)
        ).statusCode(409);
        patch("/api/v1/schema-bindings/" + binding + "/inactivate").statusCode(200);
        assertThat(resolver.resolve("FEATURE", code)).isEqualTo(defaultDefinition("FEATURE"));
        delete("/api/v1/schema-bindings/" + binding).statusCode(204);
    }

    @Test
    void sameCodeCanBeBoundInTwoResourceCategoriesAndPublisherTypesRemainDistinct() {
        String name = "contract-" + UUID.randomUUID().toString().substring(0, 8);
        String schema = post(
            "/api/v1/schemas",
            Map.of("code", name, "name", "Shared Contract", "definition", Map.of("type", "object"))
        )
            .statusCode(201)
            .extract()
            .path("identifier");
        post(
            "/api/v1/schema-bindings",
            Map.of("resourceType", "FEATURE", "resourceCode", name, "schemaIdentifier", schema)
        ).statusCode(201);
        post(
            "/api/v1/schema-bindings",
            Map.of("resourceType", "CATALOG", "resourceCode", name, "schemaIdentifier", schema)
        ).statusCode(201);
        assertThat(resolver.resolve("PUBLISHER", "WEB_SOCKET")).isEqualTo(defaultDefinition("PUBLISHER"));
        assertThat(resolver.resolve("PUBLISHER", "KAAS")).isEqualTo(defaultDefinition("PUBLISHER"));
    }

    @Test
    void newResourceTypeCanRegisterItsOwnDefaultWithoutChangingAnEnum() {
        String code = "contract-" + UUID.randomUUID().toString().substring(0, 8);
        String schema = post(
            "/api/v1/schemas",
            Map.of("code", code, "name", "Dynamic Contract", "definition", Map.of("type", "object"))
        )
            .statusCode(201)
            .extract()
            .path("identifier");
        String version = get("/api/v1/schemas/" + schema + "/versions")
            .statusCode(200)
            .extract()
            .path("[0].identifier");
        patch("/api/v1/schemas/" + schema + "/versions/" + version + "/publish").statusCode(200);

        post(
            "/api/v1/schema-bindings",
            Map.of("resourceType", "CUSTOM_DOMAIN", "resourceCode", "DEFAULT", "schemaIdentifier", schema)
        ).statusCode(201);
        post(
            "/api/v1/schema-bindings",
            Map.of("resourceType", "CUSTOM_DOMAIN", "resourceCode", "widget", "schemaIdentifier", schema)
        ).statusCode(201);
        post(
            "/api/v1/schema-bindings",
            Map.of("resourceType", "CUSTOM_DOMAIN", "resourceCode", "WIDGET", "schemaIdentifier", schema)
        ).statusCode(201);
        assertThat(resolver.resolve("CUSTOM_DOMAIN", "missing-code")).contains("\"type\"").contains("\"object\"");
    }

    @Test
    void workspaceSchemasRemainScopedAndRequirePublication() {
        String workspace = UUID.randomUUID().toString();
        jdbc.update(
            """
            insert into type_workspaces (code, label, description, sort_order, is_active, settings)
            values ('ADMIN', 'Admin', 'Administrative workspace', 1, true, '{}')
            on duplicate key update code = values(code)
            """
        );
        jdbc.update(
            """
            insert into workspaces
                (version, identifier, workspace_type_code, name, description, requester,
                 acronym, settings, authorizer_group, email_group, onboarding,
                 lifecycle_code, created_at, updated_at)
            values (0, ?, 'ADMIN', ?, 'Schema test workspace', 'testuser',
                    'SCH', null, null, 'schema-it@example.com', false, 'ACTIVE',
                    current_timestamp, current_timestamp)
            """,
            workspace,
            "Schema " + workspace.substring(0, 8)
        );
        String code = "workspace-" + workspace.substring(0, 8);
        String path = "/api/v1/workspaces/" + workspace + "/schemas";
        String schema = post(
            path,
            Map.of("code", code, "name", "Workspace Contract", "definition", Map.of("type", "object"))
        )
            .statusCode(201)
            .body("workspaceIdentifier", equalTo(workspace))
            .extract()
            .path("identifier");
        get(path + "/" + schema)
            .statusCode(200)
            .body("code", equalTo(code));
        get("/api/v1/schemas/" + schema).statusCode(404);
        assertThat(resolver.resolve("WORKSPACE", code)).isEqualTo(defaultDefinition("WORKSPACE"));
    }

    @Test
    void schemaMutationReturnsTheCommittedVersionForTheNextUpdate() {
        String code = "version-" + UUID.randomUUID().toString().substring(0, 8);
        JsonPath created = post("/api/v1/schemas", Map.of(
                "code", code, "name", "Before", "definition", Map.of("type", "object")))
                .statusCode(201).extract().jsonPath();
        String path = "/api/v1/schemas/" + created.getString("identifier");
        long initial = created.getLong("version");
        long updated = request().contentType(ContentType.JSON)
                .body(Map.of("version", initial, "name", "After"))
                .put(path).then().statusCode(200).extract().jsonPath().getLong("version");
        assertThat(updated).isEqualTo(initial + 1);
        assertThat(get(path).statusCode(200).extract().jsonPath().getLong("version")).isEqualTo(updated);
        request().contentType(ContentType.JSON).body(Map.of("version", updated, "name", "Again"))
                .put(path).then().statusCode(200);
        long inactive = patch(path + "/inactivate").statusCode(200)
                .extract().jsonPath().getLong("version");
        assertThat(get(path).statusCode(200).extract().jsonPath().getLong("version")).isEqualTo(inactive);
        long active = patch(path + "/activate").statusCode(200)
                .extract().jsonPath().getLong("version");
        assertThat(active).isEqualTo(inactive + 1);
    }

    @Test
    void bindingMutationReturnsTheCommittedVersionForTheNextUpdate() {
        String code = "binding-" + UUID.randomUUID().toString().substring(0, 8);
        String schema = post("/api/v1/schemas", Map.of(
                "code", code, "name", "Contract", "definition", Map.of("type", "object")))
                .statusCode(201).extract().path("identifier");
        JsonPath created = post("/api/v1/schema-bindings", Map.of(
                "resourceType", "FEATURE", "resourceCode", code, "schemaIdentifier", schema))
                .statusCode(201).extract().jsonPath();
        String path = "/api/v1/schema-bindings/" + created.getString("identifier");
        long inactive = patch(path + "/inactivate").statusCode(200)
                .extract().jsonPath().getLong("version");
        assertThat(inactive).isEqualTo(created.getLong("version") + 1);
        long active = patch(path + "/activate").statusCode(200)
                .extract().jsonPath().getLong("version");
        assertThat(active).isEqualTo(inactive + 1);
        long updated = request().contentType(ContentType.JSON)
                .body(Map.of("version", active, "schemaIdentifier", schema))
                .put(path).then().statusCode(200).extract().jsonPath().getLong("version");
        assertThat(updated).isEqualTo(active + 1);
        assertThat(get(path).statusCode(200).extract().jsonPath().getLong("version")).isEqualTo(updated);
    }

    @Test
    void schemaRejectsOversizedFieldsBeforePersistenceAndAcceptsStorageLimits() {
        String code = "limits-" + UUID.randomUUID().toString().substring(0, 8);
        for (String field : List.of("code", "name", "description", "versionName")) {
            Map<String, Object> body = new java.util.HashMap<>(Map.of(
                    "code", code, "name", "Contract", "definition", Map.of("type", "object")));
            int limit = switch (field) { case "code" -> 50; case "description" -> 500; default -> 100; };
            body.put(field, "x".repeat(limit + 1));
            post("/api/v1/schemas", body).statusCode(400);
        }
        assertThat(jdbc.queryForObject("select count(*) from schema_definitions where code=?",
                Integer.class, code)).isZero();
        JsonPath schema = post("/api/v1/schemas", Map.of(
                "code", "x".repeat(42) + UUID.randomUUID().toString().substring(0, 8),
                "name", "n".repeat(100), "description", "d".repeat(500),
                "versionName", "v".repeat(100), "definition", Map.of("type", "object")))
                .statusCode(201).extract().jsonPath();
        String path = "/api/v1/schemas/" + schema.getString("identifier");
        for (String field : List.of("name", "description")) {
            Map<String, Object> body = new java.util.HashMap<>(Map.of(
                    "version", schema.getLong("version"), "name", "Valid"));
            body.put(field, "x".repeat(field.equals("name") ? 101 : 501));
            request().contentType(ContentType.JSON).body(body).put(path).then().statusCode(400);
        }
        post(path + "/versions", Map.of("versionName", "v".repeat(101),
                "definition", Map.of("type", "object"))).statusCode(400);
        get(path).statusCode(200).body("name", equalTo("n".repeat(100)))
                .body("description", equalTo("d".repeat(500)));
    }

    private String defaultDefinition(String resourceType) {
        return jdbc.queryForObject(
            """
            select v.definition from schema_binding c
            join schema_versions v on v.schema_id = c.schema_id
            where c.resource_type = ? and c.resource_code = 'DEFAULT' and v.status = 'PUBLISHED'
            order by v.schema_version desc limit 1
            """,
            String.class,
            resourceType
        );
    }

    private io.restassured.specification.RequestSpecification request() {
        return given()
            .port(port)
            .header("correlation-id", "schema-binding-it")
            .header("Authorization", "Bearer schema-binding-it")
            .accept(ContentType.JSON);
    }

    private io.restassured.response.ValidatableResponse post(String path, Object body) {
        return request().contentType(ContentType.JSON).body(body).when().post(path).then();
    }

    private io.restassured.response.ValidatableResponse get(String path) {
        return request().when().get(path).then();
    }

    private io.restassured.response.ValidatableResponse patch(String path) {
        return request().when().patch(path).then();
    }

    private io.restassured.response.ValidatableResponse delete(String path) {
        return request().when().delete(path).then();
    }
}
