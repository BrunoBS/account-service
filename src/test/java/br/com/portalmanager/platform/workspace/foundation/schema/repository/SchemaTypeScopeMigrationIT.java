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
@TestPropertySource(properties = {
        "spring.flyway.target=22",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SchemaTypeScopeMigrationIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void shouldMoveSingleScopeToSchemaTypeColumn() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                insert into schema_types
                    (version, identifier, code, name, lifecycle_code, created_at, updated_at)
                values (0, UUID(), 'WORKSPACE_ONLY', 'Workspace only', 'ACTIVE',
                        current_timestamp, current_timestamp)
                """);
        Long typeId = jdbc.queryForObject(
                "select id from schema_types where code = 'WORKSPACE_ONLY'", Long.class);
        jdbc.update("""
                insert into schema_type_scopes (schema_type_id, scope_code)
                values (?, 'WORKSPACE')
                """, typeId);

        Flyway.configure().dataSource(dataSource)
                .locations("classpath:db/migration").target("23").load().migrate();

        assertThat(jdbc.queryForObject(
                "select scope_code from schema_types where code = 'WORKSPACE_ONLY'", String.class))
                .isEqualTo("WORKSPACE");
        assertThat(jdbc.queryForObject(
                "select scope_code from schema_types where code = 'DEFAULT'", String.class))
                .isEqualTo("PLATFORM");
        assertThat(jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = database() and table_name = 'schema_type_scopes'
                """, Integer.class)).isZero();
    }
}
