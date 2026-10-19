package br.com.portalmanager.platform.workspace.foundation.schema.repository;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@PlatformIntegrationTest
@Import(MigrationSchemaValidationTestConfiguration.class)
@WithMySql
@TestPropertySource(
    properties = {
        "spring.flyway.target=82",
        "platform.schema-validation.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
    }
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SchemaBindingRenameMigrationIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void renamesTableAndConstraintsWithoutLosingBindingsOrBreakingPublishedView() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update(
            """
            INSERT INTO type_life_cycle (code, label, description, sort_order, is_active, settings)
            VALUES ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')
            ON DUPLICATE KEY UPDATE code = VALUES(code)
            """
        );
        jdbc.update(
            """
            INSERT INTO type_schema_scopes (code, label, description, sort_order, is_active, settings)
            VALUES ('PLATFORM', 'Platform', 'Platform-owned schema', 1, true, '{}')
            ON DUPLICATE KEY UPDATE code = VALUES(code)
            """
        );
        jdbc.update(
            """
            INSERT INTO type_schema_version_status (code, label, description, sort_order, is_active, settings)
            VALUES ('PUBLISHED', 'Published', 'Published schema version', 2, true, '{}')
            ON DUPLICATE KEY UPDATE code = VALUES(code)
            """
        );
        jdbc.update(
            """
            INSERT INTO schema_definitions
                (version, identifier, scope_code, workspace_id, code, name, description, lifecycle_code, created_at, updated_at)
            VALUES (0, UUID(), 'PLATFORM', NULL, 'binding-migration-test', 'Binding migration test', NULL,
                    'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """
        );
        Long schemaId = jdbc.queryForObject(
            "SELECT id FROM schema_definitions WHERE code = 'binding-migration-test'",
            Long.class
        );
        jdbc.update(
            """
            INSERT INTO schema_versions (identifier, schema_id, schema_version, version_name, definition, status, created_at)
            VALUES (UUID(), ?, 1, 'v1', '{"type":"object"}', 'PUBLISHED', CURRENT_TIMESTAMP)
            """,
            schemaId
        );
        jdbc.update(
            """
            INSERT INTO schema_configuration
                (version, identifier, resource_type, resource_code, schema_id, lifecycle_code, created_at, updated_at)
            VALUES (0, UUID(), 'MIGRATION_TEST', 'binding-migration-test', ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """,
            schemaId
        );
        String identifier = jdbc.queryForObject(
            "SELECT identifier FROM schema_configuration WHERE resource_code = 'binding-migration-test'",
            String.class
        );
        assertThat(identifier).isNotBlank();

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("83").load().migrate();

        assertThat(
            jdbc.queryForObject(
                "SELECT identifier FROM schema_binding WHERE resource_code = 'binding-migration-test' AND identifier = ?",
                String.class,
                identifier
            )
        ).isEqualTo(identifier);
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'schema_configuration'",
                Integer.class
            )
        ).isZero();
        assertThat(
            jdbc.queryForObject(
                "SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'schema_binding' AND index_name = 'uk_schema_binding_resource'",
                Integer.class
            )
        ).isEqualTo(1);
        assertThat(
            jdbc.queryForObject("SELECT COUNT(*) FROM vw_platform_resource_schemas", Integer.class)
        ).isPositive();
    }
}
