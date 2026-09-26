package br.com.itau.portalmanager.workspace.foundation.schema.repository;

import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
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

    @Autowired private DataSource dataSource;
    @Autowired private Flyway flyway;

    @Test
    void shouldApplySchemaV3VersionStatusCatalog() {
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
    }
}
