package br.com.portalmanager.platform.workspace.entrypoint.web.schema;

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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class SchemaApiIT {
    @LocalServerPort private int port;
    @Autowired private AuthorizationMock authorization;
    @Autowired private SchemaResolutionPort resolver;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void prepare() {
        authorization.reset();
        authorization.allow(session -> session.groups("PM5_OWNER"));
    }

    @Test
    void schemaConfigurationFallsBackBeforePublicationAndResolvesPublishedDefinition() {
        String code = "contract-" + UUID.randomUUID().toString().substring(0, 8);
        String schema = post("/api/v1/schemas", Map.of("code", code, "name", "Platform Contract",
                "definition", Map.of("type", "object"))).statusCode(201)
                .body("scope", equalTo("PLATFORM")).extract().path("identifier");

        String binding = post("/api/v1/schema-configurations", Map.of(
                "resourceType", "FEATURE", "resourceCode", code, "schemaIdentifier", schema))
                .statusCode(201).body("resourceCode", equalTo(code)).extract().path("identifier");
        assertThat(resolver.resolve(SchemaResourceType.FEATURE, code)).isEqualTo(defaultDefinition("FEATURE"));

        String version = get("/api/v1/schemas/" + schema + "/versions").statusCode(200)
                .extract().path("[0].identifier");
        patch("/api/v1/schemas/" + schema + "/versions/" + version + "/publish")
                .statusCode(200);
        assertThat(resolver.resolve(SchemaResourceType.FEATURE, code)).contains("\"type\":\"object\"");

        post("/api/v1/schema-configurations", Map.of(
                "resourceType", "FEATURE", "resourceCode", code, "schemaIdentifier", schema))
                .statusCode(409);
        patch("/api/v1/schema-configurations/" + binding + "/inactivate").statusCode(200);
        assertThat(resolver.resolve(SchemaResourceType.FEATURE, code)).isEqualTo(defaultDefinition("FEATURE"));
        delete("/api/v1/schema-configurations/" + binding).statusCode(204);
    }

    @Test
    void sameCodeCanBeBoundInTwoResourceCategoriesAndPublisherTypesRemainDistinct() {
        String name = "contract-" + UUID.randomUUID().toString().substring(0, 8);
        String schema = post("/api/v1/schemas", Map.of("code", name, "name", "Shared Contract",
                "definition", Map.of("type", "object"))).statusCode(201)
                .extract().path("identifier");
        post("/api/v1/schema-configurations", Map.of("resourceType", "FEATURE",
                "resourceCode", name, "schemaIdentifier", schema)).statusCode(201);
        post("/api/v1/schema-configurations", Map.of("resourceType", "CATALOG",
                "resourceCode", name, "schemaIdentifier", schema)).statusCode(201);
        assertThat(resolver.resolve(SchemaResourceType.PUBLISHER, "WEB_SOCKET"))
                .isEqualTo(defaultDefinition("PUBLISHER"));
        assertThat(resolver.resolve(SchemaResourceType.PUBLISHER, "KAAS"))
                .isEqualTo(defaultDefinition("PUBLISHER"));
    }

    @Test
    void workspaceSchemasRemainScopedAndRequirePublication() {
        String workspace = UUID.randomUUID().toString();
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
                values (0, ?, 'ADMIN', ?, 'Schema test workspace', 'integration-test',
                        'SCH', null, null, 'schema-it@example.com', false, 'ACTIVE',
                        current_timestamp, current_timestamp)
                """, workspace, "Schema " + workspace.substring(0, 8));
        String code = "workspace-" + workspace.substring(0, 8);
        String path = "/api/v1/workspaces/" + workspace + "/schemas";
        String schema = post(path, Map.of("code", code, "name", "Workspace Contract",
                "definition", Map.of("type", "object")))
                .statusCode(201).body("workspaceIdentifier", equalTo(workspace))
                .extract().path("identifier");
        get(path + "/" + schema).statusCode(200).body("code", equalTo(code));
        get("/api/v1/schemas/" + schema).statusCode(404);
        assertThat(resolver.resolve(SchemaResourceType.WORKSPACE, code))
                .isEqualTo(defaultDefinition("WORKSPACE"));
    }

    private String defaultDefinition(String resourceType) {
        return jdbc.queryForObject("""
                select v.definition from schema_configuration c
                join schema_versions v on v.schema_id = c.schema_id
                where c.resource_type = ? and c.resource_code = 'DEFAULT' and v.status = 'PUBLISHED'
                order by v.schema_version desc limit 1
                """, String.class, resourceType);
    }

    private io.restassured.specification.RequestSpecification request() {
        return given().port(port).header("X-Correlation-Id", "schema-configuration-it")
                .header("Authorization", "Bearer schema-configuration-it").accept(ContentType.JSON);
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
