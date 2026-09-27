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
        "spring.flyway.target=17",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SchemaFoundationMigrationIT {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private Flyway flyway;

    @Test
    void shouldUpgradeSchemaFoundationThroughSchemaTypeDomain() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("17");

        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.columns " +
                        "where table_schema = database() " +
                        "and table_name = 'schema_definitions' " +
                        "and column_name = 'workspace_id' " +
                        "and data_type = 'bigint'",
                Integer.class
        )).isEqualTo(1);

        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.columns " +
                        "where table_schema = database() " +
                        "and table_name = 'schema_definitions' " +
                        "and column_name = 'workspace_identifier'",
                Integer.class
        )).isZero();

        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.table_constraints " +
                        "where table_schema = database() " +
                        "and table_name = 'schema_definitions' " +
                        "and constraint_name = 'fk_schema_definitions_workspace'",
                Integer.class
        )).isEqualTo(1);

        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.table_constraints " +
                        "where table_schema = database() " +
                        "and table_name = 'schema_definitions' " +
                        "and constraint_name = 'uk_schema_definitions_platform_type'",
                Integer.class
        )).isEqualTo(1);

        Flyway upgradeFlyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("18")
                .load();

        upgradeFlyway.migrate();

        assertThat(upgradeFlyway.info().current().getVersion().getVersion()).isEqualTo("18");

        assertThat(jdbc.queryForObject(
                "select count(*) from type_schema_version_status where code in ('DRAFT','PUBLISHED')",
                Integer.class
        )).isEqualTo(2);

        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.table_constraints " +
                        "where table_schema = database() " +
                        "and table_name = 'schema_versions' " +
                        "and constraint_name = 'fk_schema_versions_status'",
                Integer.class
        )).isEqualTo(1);

        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.table_constraints " +
                        "where table_schema = database() " +
                        "and table_name = 'schema_versions' " +
                        "and constraint_name = 'uk_schema_versions_single_draft'",
                Integer.class
        )).isEqualTo(1);

        jdbc.update("""
                insert into type_life_cycle
                    (code, label, description, sort_order, is_active, settings)
                values
                    ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')
                on duplicate key update code = values(code)
                """);

        jdbc.update("""
                insert into type_schema_scopes
                    (code, label, description, sort_order, is_active, settings)
                values
                    ('PLATFORM', 'Platform', 'Platform-owned schema', 1, true, '{}'),
                    ('WORKSPACE', 'Workspace', 'Workspace-owned schema', 2, true, '{}')
                on duplicate key update code = values(code)
                """);

        jdbc.update("""
                insert into type_schema_types
                    (code, label, description, sort_order, is_active, settings)
                values
                    ('DEFAULT', 'Default', 'Fallback schema type', 1, true, '{}')
                on duplicate key update code = values(code)
                """);

        Flyway schemaTypeUpgrade = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("21")
                .load();

        schemaTypeUpgrade.migrate();

        assertThat(schemaTypeUpgrade.info().current().getVersion().getVersion()).isEqualTo("21");

        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.tables " +
                        "where table_schema = database() and table_name = 'schema_types'",
                Integer.class
        )).isEqualTo(1);

        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.tables " +
                        "where table_schema = database() and table_name = 'type_schema_types'",
                Integer.class
        )).isZero();

        assertThat(jdbc.queryForObject(
                "select count(*) from schema_type_scopes sts " +
                        "join schema_types st on st.id = sts.schema_type_id " +
                        "where st.code = 'DEFAULT' and sts.scope_code = 'PLATFORM'",
                Integer.class
        )).isEqualTo(1);

        assertThat(jdbc.queryForObject(
                "select lifecycle_code from schema_types where code = 'DEFAULT'",
                String.class
        )).isEqualTo("ACTIVE");

        assertThat(jdbc.queryForObject(
                "select lifecycle_code from schema_definitions " +
                        "where schema_type_code = 'DEFAULT' and scope_code = 'PLATFORM'",
                String.class
        )).isEqualTo("ACTIVE");

        assertThat(jdbc.queryForObject(
                "select count(*) from information_schema.statistics " +
                        "where table_schema = database() " +
                        "and table_name = 'schema_types' " +
                        "and index_name = 'uk_schema_types_id'",
                Integer.class
        )).isZero();

        assertThat(jdbc.queryForObject(
                "select count(*) from schema_type_scopes sts " +
                        "join schema_types st on st.id = sts.schema_type_id " +
                        "where st.code = 'DEFAULT' and sts.scope_code = 'WORKSPACE'",
                Integer.class
        )).isZero();
    }
}
