package br.com.portalmanager.platform.workspace.foundation.catalog.resourcescopetype;

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
        "spring.flyway.target=13",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ResourceScopeMigrationIT {

    @Autowired private DataSource dataSource;
    @Autowired private Flyway flyway;

    @Test
    void shouldConsolidatePublisherScopeAndSchemaTypeIntoResourceScopeType() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("13");

        jdbc.update("""
                insert into type_publisher_scopes
                    (code, label, description, sort_order, is_active, settings)
                values
                    ('WORKSPACE', 'Workspace', 'Publisher workspace scope', 1, true, '{}')
                """);

        jdbc.update("""
                insert into type_schemas
                    (code, label, description, sort_order, is_active, settings)
                values
                    ('APPLICATION', 'Application', 'Schema application scope', 2, true, '{}')
                """);

        Flyway upgradeFlyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("14")
                .load();
        upgradeFlyway.migrate();

        assertThat(upgradeFlyway.info().current().getVersion().getVersion()).isEqualTo("14");

        assertThat(jdbc.queryForObject(
                "select count(*) from type_resource_scopes",
                Integer.class
        )).isEqualTo(2);

        assertThat(jdbc.queryForObject(
                "select label from type_resource_scopes where code = 'WORKSPACE'",
                String.class
        )).isEqualTo("Workspace");

        assertThat(jdbc.queryForObject(
                "select label from type_resource_scopes where code = 'APPLICATION'",
                String.class
        )).isEqualTo("Application");

        assertThat(tableCount(jdbc, "type_publisher_scopes")).isZero();
        assertThat(tableCount(jdbc, "type_schemas")).isZero();
        assertThat(tableCount(jdbc, "type_resource_scopes")).isEqualTo(1);
    }

    private Integer tableCount(JdbcTemplate jdbc, String tableName) {
        return jdbc.queryForObject("""
                select count(*) from information_schema.tables
                 where table_schema = database()
                   and table_name = ?
                """, Integer.class, tableName);
    }
}
