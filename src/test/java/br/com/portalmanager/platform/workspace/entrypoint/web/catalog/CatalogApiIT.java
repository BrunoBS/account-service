package br.com.portalmanager.platform.workspace.entrypoint.web.catalog;

import br.com.portalmanager.platform.library.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class CatalogApiIT {

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
        seedDefaultSchema();
    }

    private void seedDefaultSchema() {
        jdbc.update("""
                insert into type_life_cycle (code, label, description, sort_order, is_active, settings)
                values ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')
                on duplicate key update code = values(code)
                """);
        jdbc.update("""
                insert into type_schema_scopes (code, label, description, sort_order, is_active, settings)
                values ('PLATFORM', 'Platform', 'Platform-owned schema', 1, true, '{}')
                on duplicate key update code = values(code)
                """);
        jdbc.update("""
                insert into type_schema_version_status
                    (code, label, description, sort_order, is_active, settings)
                values ('PUBLISHED', 'Published', 'Published schema version', 2, true, '{}')
                on duplicate key update code = values(code)
                """);
        jdbc.update("""
                insert into schema_types
                    (version, identifier, code, name, description, lifecycle_code, created_at, updated_at)
                values (0, UUID(), 'DEFAULT', 'Default', 'Fallback schema type',
                        'ACTIVE', current_timestamp, current_timestamp)
                on duplicate key update lifecycle_code = 'ACTIVE'
                """);
        Long typeId = jdbc.queryForObject(
                "select id from schema_types where code = 'DEFAULT'", Long.class);
        jdbc.update("""
                insert into schema_type_scopes (schema_type_id, scope_code)
                values (?, 'PLATFORM')
                on duplicate key update scope_code = 'PLATFORM'
                """, typeId);
        jdbc.update("""
                insert into schema_definitions
                    (version, identifier, schema_type_code, scope_code, workspace_id,
                     code, name, description, lifecycle_code, created_at, updated_at)
                values (0, UUID(), 'DEFAULT', 'PLATFORM', null, 'default', 'Default',
                        'Fallback schema', 'ACTIVE', current_timestamp, current_timestamp)
                on duplicate key update lifecycle_code = 'ACTIVE'
                """);
        Long schemaId = jdbc.queryForObject("""
                select id from schema_definitions
                where schema_type_code = 'DEFAULT' and scope_code = 'PLATFORM'
                """, Long.class);
        jdbc.update("""
                insert into schema_versions
                    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
                values (UUID(), ?, 1, 'v1', '{"type":"object","additionalProperties":true}',
                        'PUBLISHED', current_timestamp)
                on duplicate key update status = 'PUBLISHED'
                """, schemaId);
    }

    @Test
    void shouldExposeCompleteCrudForStandardAndDynamicCatalogs() {
        List<CatalogCase> catalogs = List.of(
                new CatalogCase("/api/v1/application-scope-type", "BACKEND"),
                new CatalogCase("/api/v1/authorization-type", "DEV"),
                new CatalogCase("/api/v1/environment-type", "DEFAULT"),
                new CatalogCase("/api/v1/onboarding-type", "WORKSPACE_REGISTRATION"),
                new CatalogCase("/api/v1/tag-origin-type", "MANUAL"),
                new CatalogCase("/api/v1/visibility-type", "PRIVATE"),
                new CatalogCase("/api/v1/resource-scope-type", "WORKSPACE"),
                new CatalogCase("/api/v1/share-status-type", "NOT_REQUESTED")
        );

        int order = 1;
        for (CatalogCase catalog : catalogs) {
            Map<String, Object> create = standardBody(
                    catalog.code(),
                    catalog.code() + " label",
                    "Descrição válida de " + catalog.code(),
                    order++
            );

            String code = post(catalog.path(), create)
                    .statusCode(201)
                    .body("code", equalTo(catalog.code()))
                    .extract()
                    .path("code");

            get(catalog.path() + "/" + code)
                    .statusCode(200)
                    .body("code", equalTo(code));

            Map<String, Object> update = standardBody(
                    catalog.code(),
                    catalog.code() + " atualizado",
                    "Descrição atualizada de " + catalog.code(),
                    order++
            );

            put(catalog.path() + "/" + code, update)
                    .statusCode(200)
                    .body("code", equalTo(code))
                    .body("label", equalTo(catalog.code() + " atualizado"));

            delete(catalog.path() + "/" + code).statusCode(204);

            post(catalog.path() + "/" + code + "/restore")
                    .statusCode(200)
                    .body("code", equalTo(code))
                    .body("code", equalTo(catalog.code()));
        }
    }

    @Test
    void shouldPersistCatalogSpecificConfigurationInSettings() {
        Map<String, Object> resourceScope = standardBody(
                "APPLICATION",
                "Application",
                "Descrição válida de application resource scope",
                1
        );
        resourceScope.put("settings", Map.of("resource", "APPLICATION"));

        String resourceScopeCode = post("/api/v1/resource-scope-type", resourceScope)
                .statusCode(201)
                .body("code", equalTo("APPLICATION"))
                .body("settings.resource", equalTo("APPLICATION"))
                .extract()
                .path("code");

        get("/api/v1/resource-scope-type/" + resourceScopeCode)
                .statusCode(200)
                .body("settings.resource", equalTo("APPLICATION"));

        Map<String, Object> onboarding = standardBody(
                "WORKSPACE_FIRST_ENVIRONMENT",
                "Workspace first environment",
                "Descrição válida da fase de onboarding",
                3
        );
        onboarding.put("settings", Map.of("orientation", "Orientação inicial"));

        post("/api/v1/onboarding-type", onboarding)
                .statusCode(201)
                .body("settings.orientation", equalTo("Orientação inicial"));
    }

    @Test
    void shouldRejectUnknownEnumNameAndInvalidSettingsShape() {
        seedWorkspaceTypeSchema();
        post(
                "/api/v1/workspace-type",
                standardBody("UNKNOWN", "Unknown", "Descrição válida de tipo desconhecido", 1)
        )
                .statusCode(400)
                .body("details.field", hasItem("code"));

        Map<String, Object> invalidSettings = standardBody(
                "MANAGER",
                "Manager",
                "Descrição válida de manager",
                2
        );
        invalidSettings.put("settings", Map.of("nested", Map.of("not", "scalar")));

        post("/api/v1/workspace-type", invalidSettings)
                .statusCode(400)
                .body("details.field", hasItem("settings"));
    }

    private void seedWorkspaceTypeSchema() {
        jdbc.update("""
                insert into schema_types
                    (version, identifier, code, name, description, lifecycle_code, created_at, updated_at)
                values (0, UUID(), 'WORKSPACE_TYPE', 'Workspace type', null,
                        'ACTIVE', current_timestamp, current_timestamp)
                on duplicate key update lifecycle_code = 'ACTIVE'
                """);
        Long typeId = jdbc.queryForObject(
                "select id from schema_types where code = 'WORKSPACE_TYPE'", Long.class);
        jdbc.update("""
                insert into schema_type_scopes (schema_type_id, scope_code)
                values (?, 'PLATFORM')
                on duplicate key update scope_code = 'PLATFORM'
                """, typeId);
        jdbc.update("""
                insert into schema_definitions
                    (version, identifier, schema_type_code, scope_code, workspace_id,
                     code, name, description, lifecycle_code, created_at, updated_at)
                values (0, UUID(), 'WORKSPACE_TYPE', 'PLATFORM', null,
                        'workspace-type', 'Workspace type settings', null,
                        'ACTIVE', current_timestamp, current_timestamp)
                on duplicate key update lifecycle_code = 'ACTIVE'
                """);
        Long schemaId = jdbc.queryForObject("""
                select id from schema_definitions
                where schema_type_code = 'WORKSPACE_TYPE' and scope_code = 'PLATFORM'
                """, Long.class);
        jdbc.update("""
                insert into schema_versions
                    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
                values (UUID(), ?, 1, 'v1',
                        '{"type":"object","additionalProperties":{"type":"string"}}',
                        'PUBLISHED', current_timestamp)
                on duplicate key update status = 'PUBLISHED'
                """, schemaId);
    }

    @Test
    void shouldRequireOwnerAuthorizationForCatalogAdministration() {
        authorizationMock.reset();
        authorizationMock.allow(session -> session.groups("USER"));

        post(
                "/api/v1/application-scope-type",
                standardBody("BACKEND", "Backend", "Descrição válida de backend", 1)
        ).statusCode(201);

        authorizationMock.verifyCalledWithPolicy("OWNER");
    }

    private Map<String, Object> standardBody(
            String code,
            String label,
            String description,
            Integer sortOrder
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("label", label);
        body.put("description", description);
        body.put("sortOrder", sortOrder);
        body.put("settings", Map.of());
        return body;
    }

    private ValidatableResponse get(String path) {
        return authorized().when().get(path).then();
    }

    private ValidatableResponse post(String path, Map<String, Object> body) {
        return authorized()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post(path)
                .then();
    }

    private ValidatableResponse post(String path) {
        return authorized()
                .when()
                .post(path)
                .then();
    }

    private ValidatableResponse put(String path, Map<String, Object> body) {
        return authorized()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .put(path)
                .then();
    }

    private ValidatableResponse delete(String path) {
        return authorized()
                .when()
                .delete(path)
                .then();
    }

    private io.restassured.specification.RequestSpecification authorized() {
        return given()
                .port(port)
                .header("X-Correlation-Id", "catalog-api-it")
                .header("Authorization", "Bearer catalog-api-it")
                .accept(ContentType.JSON);
    }

    private record CatalogCase(String path, String code) {
    }
}
