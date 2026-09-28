package br.com.portalmanager.platform.workspace.support;

import org.springframework.jdbc.core.JdbcTemplate;

/** Recreates the persisted V17/V28 fallback records after test database cleanup. */
public final class SchemaDefaultFixture {
    private SchemaDefaultFixture() { }

    public static void seed(JdbcTemplate jdbc) {
        jdbc.update("""
                INSERT IGNORE INTO type_life_cycle
                    (code, label, description, sort_order, is_active, settings)
                VALUES ('ACTIVE', 'Active', 'Active lifecycle', 1, true, '{}')
                """);
        jdbc.update("""
                INSERT IGNORE INTO type_schema_scopes
                    (code, label, description, sort_order, is_active, settings)
                VALUES ('PLATFORM', 'Platform', 'Platform scope', 1, true, '{}')
                """);
        jdbc.update("""
                INSERT IGNORE INTO type_schema_version_status
                    (code, label, description, sort_order, is_active, settings)
                VALUES ('DRAFT', 'Draft', 'Draft schema version', 1, true, '{}'),
                       ('PUBLISHED', 'Published', 'Published schema version', 2, true, '{}')
                """);
        jdbc.update("""
                INSERT IGNORE INTO schema_definitions
                    (version, identifier, scope_code, code, name, lifecycle_code, created_at, updated_at)
                VALUES (0, UUID(), 'PLATFORM', 'default', 'Default', 'ACTIVE',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT IGNORE INTO schema_versions
                    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
                SELECT UUID(), id, 1, 'Default', '{"type":"object","additionalProperties":true}',
                       'PUBLISHED', CURRENT_TIMESTAMP
                FROM schema_definitions WHERE scope_code = 'PLATFORM' AND code = 'default'
                """);
        jdbc.update("""
                INSERT IGNORE INTO schema_configuration
                    (version, identifier, resource_type, resource_code, schema_id, lifecycle_code,
                     created_at, updated_at)
                SELECT 0, UUID(), resource_types.resource_type, 'DEFAULT', sd.id, 'ACTIVE',
                       CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
                FROM schema_definitions sd
                CROSS JOIN (
                    SELECT 'WORKSPACE' AS resource_type UNION ALL
                    SELECT 'APPLICATION' UNION ALL
                    SELECT 'ENVIRONMENT' UNION ALL
                    SELECT 'PUBLISHER' UNION ALL
                    SELECT 'FEATURE' UNION ALL
                    SELECT 'MICROSERVICE' UNION ALL
                    SELECT 'CATALOG'
                ) resource_types
                WHERE sd.scope_code = 'PLATFORM' AND sd.code = 'default'
                """);
    }
}
