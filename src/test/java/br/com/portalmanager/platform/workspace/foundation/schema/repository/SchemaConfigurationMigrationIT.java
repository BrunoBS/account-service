package br.com.portalmanager.platform.workspace.foundation.schema.repository;

import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@PlatformIntegrationTest
@WithMySql
@TestPropertySource(properties = {"spring.flyway.target=27", "platform.schema-validation.enabled=false", "spring.jpa.hibernate.ddl-auto=none"})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SchemaConfigurationMigrationIT {
    @Autowired private DataSource dataSource;

    @Test
    void migratesExistingPlatformBindingAndRemovesLegacyTypeTables() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO type_life_cycle (code, label, description, sort_order, is_active, settings)
                VALUES ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')
                ON DUPLICATE KEY UPDATE code = VALUES(code)
                """);
        jdbc.update("""
                INSERT INTO type_schema_scopes (code, label, description, sort_order, is_active, settings)
                VALUES ('PLATFORM', 'Platform', 'Platform-owned schema', 1, true, '{}')
                ON DUPLICATE KEY UPDATE code = VALUES(code)
                """);
        jdbc.update("""
                INSERT INTO schema_types
                    (version, identifier, code, name, lifecycle_code, scope_code, created_at, updated_at)
                VALUES (0, UUID(), 'DEFAULT', 'Default', 'ACTIVE', 'PLATFORM',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE code = VALUES(code)
                """);
        jdbc.update("""
                INSERT INTO schema_definitions
                    (version, identifier, schema_type_code, scope_code, code, name, lifecycle_code,
                     created_at, updated_at)
                VALUES (0, UUID(), 'DEFAULT', 'PLATFORM', 'default', 'Default', 'ACTIVE',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE code = VALUES(code)
                """);
        jdbc.update("""
                INSERT INTO type_schema_version_status
                    (code, label, description, sort_order, is_active, settings)
                VALUES ('PUBLISHED', 'Published', 'Published schema version', 2, true, '{}')
                ON DUPLICATE KEY UPDATE code = VALUES(code)
                """);
        jdbc.update("""
                INSERT INTO schema_versions
                    (identifier, schema_id, schema_version, version_name, definition, status, created_at)
                SELECT UUID(), id, 1, 'Default', '{}', 'PUBLISHED', CURRENT_TIMESTAMP
                FROM schema_definitions WHERE schema_type_code = 'DEFAULT'
                ON DUPLICATE KEY UPDATE status = VALUES(status)
                """);
        jdbc.update("""
                INSERT INTO schema_types
                    (version, identifier, code, name, description, lifecycle_code, scope_code, created_at, updated_at)
                VALUES (0, UUID(), 'WORKSPACE_TYPE', 'Workspace catalog', null, 'ACTIVE', 'PLATFORM',
                        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        jdbc.update("""
                INSERT INTO schema_definitions
                    (version, identifier, schema_type_code, scope_code, code, name, lifecycle_code, created_at, updated_at)
                VALUES (0, UUID(), 'WORKSPACE_TYPE', 'PLATFORM', 'workspace-type',
                        'Workspace catalog settings', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """);
        Long id = jdbc.queryForObject("SELECT id FROM schema_definitions WHERE schema_type_code = 'WORKSPACE_TYPE'",
                Long.class);

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("28")
                .load().migrate();

        assertThat(jdbc.queryForObject("""
                SELECT schema_id FROM schema_configuration
                WHERE resource_type = 'CATALOG' AND resource_code = 'workspace-type'
                """, Long.class)).isEqualTo(id);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM schema_configuration WHERE resource_code = 'DEFAULT'
                """, Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM schema_versions v
                JOIN schema_configuration c ON c.schema_id = v.schema_id
                WHERE c.resource_type = 'PUBLISHER' AND c.resource_code = 'DEFAULT'
                  AND v.status = 'PUBLISHED'
                """, Integer.class)).isPositive();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = DATABASE() AND table_name = 'schema_types'
                """, Integer.class)).isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = 'schema_definitions'
                  AND column_name = 'schema_type_code'
                """, Integer.class)).isZero();
    }
}
