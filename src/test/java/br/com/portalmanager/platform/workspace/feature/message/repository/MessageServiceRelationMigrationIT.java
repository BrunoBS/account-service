package br.com.portalmanager.platform.workspace.feature.message.repository;

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
@TestPropertySource(properties = {
        "spring.flyway.target=15",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class MessageServiceRelationMigrationIT {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private Flyway flyway;

    @Test
    void shouldMigrateMessageServiceCodeToStructuralServiceRelation() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("15");

        jdbc.update("""
                insert into type_life_cycle
                    (code, label, description, sort_order, is_active, settings)
                values
                    ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}')
                on duplicate key update code = values(code)
                """);

        jdbc.update("""
                insert into platform_services
                    (version, identifier, code, name, description, lifecycle_code, created_at, updated_at)
                values
                    (0, '11111111-1111-1111-1111-111111111111', 'workspace-service',
                     'Workspace Service', 'Workspace owner', 'ACTIVE', now(), now())
                """);

        jdbc.update("""
                insert into messages
                    (version, identifier, service_code, message_key, code, http_status,
                     lifecycle_code, observation, created_at, updated_at)
                values
                    (0, '22222222-2222-2222-2222-222222222222', 'workspace-service',
                     'workspace.not-found', 'WORKSPACE-0001', 404, 'ACTIVE',
                     'Migration reference', now(), now())
                """);

        Flyway upgradeFlyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("16")
                .load();
        upgradeFlyway.migrate();

        assertThat(upgradeFlyway.info().current().getVersion().getVersion()).isEqualTo("16");

        Long serviceId = jdbc.queryForObject(
                "select id from platform_services where code = 'workspace-service'",
                Long.class
        );

        assertThat(jdbc.queryForObject(
                "select service_id from messages where identifier = '22222222-2222-2222-2222-222222222222'",
                Long.class
        )).isEqualTo(serviceId);

        assertThat(columnCount(jdbc, "messages", "service_code")).isZero();
        assertThat(columnCount(jdbc, "messages", "service_id")).isEqualTo(1);
    }

    private Integer columnCount(JdbcTemplate jdbc, String tableName, String columnName) {
        return jdbc.queryForObject("""
                select count(*)
                  from information_schema.columns
                 where table_schema = database()
                   and table_name = ?
                   and column_name = ?
                """, Integer.class, tableName, columnName);
    }
}
