package br.com.portalmanager.platform.workspace.entrypoint.lifecycle;

import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.feature.Feature;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.microservice.Microservice;
import br.com.portalmanager.platform.workspace.feature.servicecontext.domain.featuregroup.FeatureGroup;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.authorization.AuthorizationMock;
import br.com.portalmanager.platform.library.testing.authorization.annotation.WithMockAuthorization;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.SchemaDefaultFixture;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@PlatformIntegrationTest
@WithMySql
@WithMockAuthorization
class LifecycleListingIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AuthorizationMock authorization;

    @BeforeEach
    void prepare() {
        SchemaDefaultFixture.seed(jdbc);
        jdbc.update(
            """
            INSERT IGNORE INTO type_life_cycle (code, label, description, sort_order, is_active, settings)
            VALUES ('QUARANTINED', 'Quarantined', 'Quarantined', 3, true, '{}')
            """);
        jdbc.update(
            """
            INSERT IGNORE INTO type_resource_scopes (code, label, description, sort_order, is_active, settings)
            VALUES ('WORKSPACE', 'Workspace', 'Workspace', 1, true, '{}')
            """
            );
        authorization.reset();
        authorization.allow(session -> session.userName("test-user").groups("PM5_OWNER"));
        jdbc.update(
            """
            INSERT IGNORE INTO type_workspaces (code, label, description, sort_order, is_active, settings)
            VALUES ('MANAGER', 'Manager', 'Manager', 1, true, '{}')
            """);
        jdbc.update(
            """
            INSERT IGNORE INTO type_application_scopes (code, label, description, sort_order, is_active, settings)
            VALUES ('SHARED', 'Shared', 'Shared', 1, true, '{}')
            """);
        jdbc.update(
            """
            INSERT IGNORE INTO type_authorizations (code, label, description, sort_order, is_active, settings)
            VALUES ('DEV', 'Dev', 'Dev', 1, true, '{}')
            """);
        jdbc.update(
            """
            INSERT INTO workspaces (identifier, workspace_type_code, name, description, requester, acronym, settings, email_group, lifecycle_code, created_at, updated_at)
            VALUES ('filter-owner', 'MANAGER', 'Filter owner', 'Owner', 'owner', 'OWN', '{}', 'owner@test', 'ACTIVE', NOW(6), NOW(6))
            """);
        jdbc.update(
            """
            INSERT INTO applications (identifier, workspace_id, name, alias, acronym, application_scope_code, settings, lifecycle_code, created_at, updated_at)
            SELECT 'filter-owner-app', id, 'Filter owner app', 'owner', 'OWN', 'SHARED', '{}', 'ACTIVE', NOW(6), NOW(6)
            FROM workspaces
            WHERE identifier='filter-owner'
            """);
        for (String lifecycle : List.of("ACTIVE", "INACTIVE", "QUARANTINED")) {
            String suffix = lifecycle.toLowerCase(Locale.ROOT);
            jdbc.update(
                """
                INSERT INTO workspaces (identifier, workspace_type_code, name, description, requester, acronym, settings, email_group, lifecycle_code, created_at, updated_at)
                VALUES (?, 'MANAGER', ?, 'Filter', 'owner', 'FLT', '{}', 'filter@test', ?, NOW(6), NOW(6))
                """, "filter-workspace-" + suffix, "Workspace " + suffix, lifecycle);
            jdbc.update(
                """
                INSERT INTO applications (identifier, workspace_id, name, alias, acronym, application_scope_code, settings, lifecycle_code, created_at, updated_at)
                SELECT ?, id, ?, 'filter', 'FLT', 'SHARED', '{}', ?, NOW(6), NOW(6)
                FROM workspaces
                WHERE identifier='filter-owner'
                """, "filter-app-" + suffix, "App " + suffix, lifecycle);
            jdbc.update(
                """
                INSERT INTO publishers (identifier, code, name, description, publisher_scope, settings, lifecycle_code, created_at, updated_at)
                VALUES (?, ?, ?, 'Filter', 'WORKSPACE', '{}', ?, NOW(6), NOW(6))
                """, "filter-publisher-" + suffix, "FILTER_" + lifecycle, "Publisher " + suffix, lifecycle);
            jdbc.update(
                """
                INSERT INTO service_context_microservices (identifier, code, name, settings, lifecycle_code, created_at, updated_at)
                VALUES (?, ?, ?, '{}', ?, NOW(6), NOW(6))
                """, "filter-microservice-" + suffix, "filter-" + suffix, "Microservice " + suffix, lifecycle);
            jdbc.update(
                """
                INSERT INTO feature_group (identifier, code, name, lifecycle_code, created_at, updated_at)
                VALUES (?, ?, ?, ?, NOW(6), NOW(6))
                """, "filter-context-" + suffix, "filter-" + suffix, "FeatureGroup " + suffix, lifecycle);
            jdbc.update(
                """
                INSERT INTO service_context_features (identifier, code, name, microservice_id, lifecycle_code, shareable, settings, created_at, updated_at)
                SELECT ?, ?, ?, id, ?, true, '{}', NOW(6), NOW(6)
                FROM service_context_microservices
                WHERE identifier='filter-microservice-active'
                """, "filter-feature-" + suffix, "filter-" + suffix, "Feature " + suffix, lifecycle);
            if (!"QUARANTINED".equals(lifecycle)) {
                jdbc.update(
                    """
                    INSERT INTO messages (identifier, microservice_id, message_key, code, http_status, lifecycle_code, created_at, updated_at)
                    SELECT ?, id, ?, ?, 400, ?, NOW(6), NOW(6)
                    FROM service_context_microservices
                    WHERE identifier='filter-microservice-active'
                    """, "filter-message-" + suffix, "filter." + suffix, "FILTER-" + lifecycle, lifecycle);
                jdbc.update(
                    """
                    INSERT INTO message_translators (identifier, message_id, locale, title, detail, suggestion, lifecycle_code, created_at, updated_at)
                    SELECT ?, id, ?, 'Filter', 'Filter', 'Filter', ?, NOW(6), NOW(6)
                    FROM messages
                    WHERE identifier='filter-message-active'
                    """, "filter-translation-" + suffix, switch (lifecycle) { case "ACTIVE" -> "en-US"; case "INACTIVE" -> "pt-BR"; default -> "es-ES"; }, lifecycle);
            }
            jdbc.update(
                """
                INSERT INTO schema_definitions (identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
                VALUES (?, 'PLATFORM', ?, ?, ?, NOW(6), NOW(6))
                """, "filter-schema-" + suffix, "filter-" + suffix, "Schema " + suffix, lifecycle);
            jdbc.update(
                """
                INSERT INTO schema_definitions (identifier, scope_code, workspace_id, code, name, lifecycle_code, created_at, updated_at)
                SELECT ?, 'WORKSPACE', id, ?, ?, ?, NOW(6), NOW(6)
                FROM workspaces
                WHERE identifier='filter-owner'
                """, "filter-workspace-schema-" + suffix, "filter-" + suffix, "Workspace schema " + suffix, lifecycle);
            jdbc.update(
                """
                INSERT INTO schema_binding (identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
                SELECT ?, 'FILTER', ?, id, ?, NOW(6), NOW(6)
                FROM schema_definitions
                WHERE identifier='filter-schema-active'
                """, "filter-configuration-" + suffix, "filter-" + suffix, lifecycle);
            jdbc.update(
                """
                INSERT INTO resource_sharing_contracts (identifier, owner_workspace_id, owner_application_id, feature_id, lifecycle_code, created_at, updated_at)
                SELECT ?, a.workspace_id, a.id, f.id, ?, NOW(6), NOW(6)
                FROM applications a
                JOIN service_context_features f ON f.identifier=?
                WHERE a.identifier='filter-owner-app'
                """, "filter-contract-" + suffix, lifecycle, "filter-feature-" + suffix);
        }
        jdbc.update(
            """
            INSERT IGNORE INTO environment_types (code, name, description, root_allowed, allows_children, workspace_required, lifecycle_code, display_order, created_at, updated_at)
            VALUES ('DEFAULT', 'Default', 'Default', true, true, false, 'ACTIVE', 1, NOW(6), NOW(6)), ('CUSTOM', 'Custom', 'Custom', true, true, true, 'INACTIVE', 2, NOW(6), NOW(6)), ('SHARD', 'Shard', 'Shard', false, true, true, 'QUARANTINED', 3, NOW(6), NOW(6)), ('CELL', 'Cell', 'Cell', false, false, true, 'ACTIVE', 4, NOW(6), NOW(6))
            """);
        jdbc.update(
            """
            UPDATE environment_types
            SET lifecycle_code=CASE code WHEN 'CUSTOM' THEN 'INACTIVE' WHEN 'SHARD' THEN 'QUARANTINED' ELSE 'ACTIVE' END
            """);
        for (String lifecycle : List.of("ACTIVE", "INACTIVE", "QUARANTINED")) {
            String suffix = lifecycle.toLowerCase(Locale.ROOT);
            jdbc.update(
                """
                INSERT INTO environments (identifier, environment_type_code, name, description, authorization_type_code, settings, sort_order, lifecycle_code, created_at, updated_at)
                SELECT ?, code, ?, 'Filter', 'DEV', '{}', 1, ?, NOW(6), NOW(6)
                FROM environment_types
                WHERE code='DEFAULT'
                """, "filter-default-env-" + suffix, "Default " + suffix, lifecycle);
            jdbc.update(
                """
                INSERT INTO environments (identifier, workspace_id, environment_type_code, name, description, authorization_type_code, settings, sort_order, lifecycle_code, created_at, updated_at)
                SELECT ?, w.id, t.code, ?, 'Filter', 'DEV', '{}', 1, ?, NOW(6), NOW(6)
                FROM workspaces w
                JOIN environment_types t ON t.code='DEFAULT'
                WHERE w.identifier='filter-owner'
                """, "filter-custom-env-" + suffix, "Custom " + suffix, lifecycle);
        }
        jdbc.update(
            """
            DELETE
            FROM environment_type_compatibilities
            """);
        jdbc.update(
            """
            INSERT INTO environment_type_compatibilities (identifier, parent_type_code, child_type_code, lifecycle_code, created_at, updated_at)
            SELECT CONCAT('filter-compat-', LOWER(c.code)), p.code, c.code, CASE c.code WHEN 'CUSTOM' THEN 'ACTIVE' WHEN 'SHARD' THEN 'INACTIVE' ELSE 'QUARANTINED' END, NOW(6), NOW(6)
            FROM environment_types p
            JOIN environment_types c ON c.code IN ('CUSTOM', 'SHARD', 'CELL')
            WHERE p.code='DEFAULT'
            """);
    }

    @Test
    void filtersEveryGeneralLifecycleListingAndDefaultsToActive() {
        for (Map.Entry<String, String> listing : listings().entrySet()) {
            for (String lifecycle : List.of("ACTIVE", "INACTIVE", "QUARANTINED")) {
                List<String> lifecycles = get(listing.getKey(), lifecycle).statusCode(200).extract().path(listing.getValue());
                if (listing.getKey().startsWith("/api/v1/dynamic-bundles/messages") && "QUARANTINED".equals(lifecycle)) {
                    assertThat(lifecycles).as(listing.getKey() + " " + lifecycle).isEmpty();
                } else {
                    assertThat(lifecycles).as(listing.getKey() + " " + lifecycle).isNotEmpty().containsOnly(lifecycle);
                }
            }
            List<String> defaults = get(listing.getKey(), null).statusCode(200).extract().path(listing.getValue());
            assertThat(defaults).as(listing.getKey() + " default").isNotEmpty().containsOnly("ACTIVE");
            get(listing.getKey(), " active ").statusCode(200);
            get(listing.getKey(), "invalid-state").statusCode(400);
            get(listing.getKey(), "").statusCode(400);
        }
    }

    @Test
    void combinesLifecycleWithContextAndPagination() {
        jdbc.update(
            """
            INSERT INTO feature_group_feature (feature_id, feature_group_id)
            SELECT f.id, c.id
            FROM service_context_features f
            JOIN feature_group c ON c.identifier='filter-context-active'
            WHERE f.identifier='filter-feature-inactive'
            """);
        List<String> identifiers = request().queryParam("lifecycle", "INACTIVE").queryParam("featureGroupIdentifier", "filter-context-active")
            .get("/api/v1/service-contexts/features").then().statusCode(200).extract().path("content.identifier");
        assertThat(identifiers).containsExactly("filter-feature-inactive");
        request().queryParam("lifecycle", "QUARANTINED").queryParam("size", 1).get("/api/v1/workspaces")
            .then().statusCode(200).body("page.totalElements", org.hamcrest.Matchers.equalTo(1));
    }

    private Map<String, String> listings() {
        return Map.ofEntries(
            Map.entry("/api/v1/workspaces", "content.lifecycle"),
            Map.entry("/api/v1/workspaces/filter-owner/applications", "content.lifecycle"),
            Map.entry("/api/v1/publishers", "content.lifecycle"),
            Map.entry("/api/v1/service-contexts/microservices", "content.lifecycle"),
            Map.entry("/api/v1/service-contexts/features", "content.lifecycle"),
            Map.entry("/api/v1/service-contexts/feature-groups", "content.lifecycle"),
            Map.entry("/api/v1/dynamic-bundles/messages", "content.lifecycle"),
            Map.entry("/api/v1/dynamic-bundles/messages/filter-message-active/translators", "content.lifecycle"),
            Map.entry("/api/v1/schemas", "lifecycle"),
            Map.entry("/api/v1/workspaces/filter-owner/schemas", "lifecycle"),
            Map.entry("/api/v1/schema-bindings", "lifecycle"),
            Map.entry("/api/v1/environment-types", "lifecycle"),
            Map.entry("/api/v1/environment-types/compatibilities", "lifecycle"),
            Map.entry("/api/v1/environment-defaults", "lifecycle"),
            Map.entry("/api/v1/workspaces/filter-owner/environments", "lifecycle"),
            Map.entry("/api/v1/workspaces/filter-owner/applications/filter-owner-app/resource-sharing/contracts", "content.lifecycle")
        );
    }

    private RequestSpecification request() {
        return given().port(port).header("correlation-id", "lifecycle-it")
            .header("Authorization", "Bearer lifecycle-it").accept(ContentType.JSON);
    }

    private ValidatableResponse get(String path, String lifecycle) {
        RequestSpecification request = request();
        if (lifecycle != null) request.queryParam("lifecycle", lifecycle);
        return request.get(path).then();
    }
}
