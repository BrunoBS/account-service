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

        Flyway upgradeFlyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("18")
                .load();

        upgradeFlyway.migrate();

        assertThat(upgradeFlyway.info().current().getVersion().getVersion()).isEqualTo("18");

        assertThat(jdbc.queryForObject(
                "select count(*) from type_schema_scopes where code in ('PLATFORM','WORKSPACE')",
                Integer.class
        )).isEqualTo(2);

        assertThat(jdbc.queryForObject(
                "select count(*) from type_schema_types where code = 'DEFAULT' and is_active = true",
                Integer.class
        )).isEqualTo(1);

        assertThat(jdbc.queryForObject(
                "select count(*) from type_schema_version_status where code in ('DRAFT','PUBLISHED')",
                Integer.class
        )).isEqualTo(2);

        assertThat(jdbc.queryForObject("""
                select count(*)
                  from schema_definitions s
                 where s.schema_type_code = 'DEFAULT'
                   and s.scope_code = 'PLATFORM'
                   and s.workspace_identifier is null
                   and s.code = 'default'
                   and s.lifecycle_code = 'ACTIVE'
                """, Integer.class
        )).isEqualTo(1);

        assertThat(jdbc.queryForObject("""
                select count(*)
                  from schema_versions sv
                  join schema_definitions s on s.id = sv.schema_id
                 where s.code = 'default'
                   and sv.schema_version = 1
                   and sv.status = 'PUBLISHED'
                """, Integer.class
        )).isEqualTo(1);
    }
}
