package br.com.portalmanager.platform.workspace.foundation.schema.repository;

import br.com.portalmanager.platform.library.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.annotation.WithMySql;
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
@TestPropertySource(properties = {"spring.flyway.target=26", "spring.jpa.hibernate.ddl-auto=none"})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SchemaConfigurationMigrationIT {
    @Autowired private DataSource dataSource;

    @Test
    void migratesExistingPlatformBindingAndRemovesLegacyTypeTables() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
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

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("27")
                .load().migrate();

        assertThat(jdbc.queryForObject("""
                SELECT schema_id FROM schema_configuration
                WHERE resource_type = 'CATALOG' AND resource_code = 'workspace-type'
                """, Long.class)).isEqualTo(id);
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
