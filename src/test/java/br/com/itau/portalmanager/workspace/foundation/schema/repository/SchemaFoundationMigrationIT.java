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
        "spring.flyway.target=16",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SchemaFoundationMigrationIT {

    @Autowired private DataSource dataSource;
    @Autowired private Flyway flyway;

    @Test
    void shouldCreateSchemaV2FoundationAndSeedDefaultPublishedSchema() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("16");

        Flyway upgradeFlyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("17")
                .load();

        upgradeFlyway.migrate();

        assertThat(upgradeFlyway.info().current().getVersion().getVersion()).isEqualTo("17");

        assertThat(jdbc.queryForObject(
                "select count(*) from type_schema_scopes where code in ('PLATFORM','WORKSPACE')",
                Integer.class
        )).isEqualTo(2);

        assertThat(jdbc.queryForObject(
                "select count(*) from schema_types where code = 'default' and lifecycle_code = 'ACTIVE'",
                Integer.class
        )).isEqualTo(1);

        assertThat(jdbc.queryForObject("""
                select count(*)
                  from schema_definitions s
                  join schema_types st on st.id = s.schema_type_id
                 where st.code = 'default'
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
