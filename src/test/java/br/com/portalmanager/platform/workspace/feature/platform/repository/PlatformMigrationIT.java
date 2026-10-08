package br.com.portalmanager.platform.workspace.feature.platform.repository;

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
        "spring.flyway.target=12",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlatformMigrationIT {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private Flyway flyway;

    @Test
    void shouldMigrateFeatureScopesToFeatureContexts() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("12");

        jdbc.update("""
                insert into type_life_cycle
                    (code, label, description, sort_order, is_active, settings)
                values
                    ('ACTIVE', 'Active', 'Active lifecycle state', 1, true, '{}'),
                    ('INACTIVE', 'Inactive', 'Inactive lifecycle state', 2, true, '{}'),
                    ('QUARANTINED', 'Quarantined', 'Quarantined lifecycle state', 3, true, '{}')
                on duplicate key update code = values(code)
                """);

        jdbc.update("""
                insert into type_feature_scopes (code, label, description, sort_order, is_active, settings)
                values ('MANAGER_ACCOUNT', 'Manager account', 'Manager context', 1, true, '{}'),
                       ('ADMIN_ACCOUNT', 'Admin account', 'Admin context', 2, false, '{}')
                """);

        jdbc.update("""
                insert into platform_services
                    (version, identifier, code, name, description, lifecycle_code, created_at, updated_at)
                values
                    (0, '11111111-1111-1111-1111-111111111111', 'portal-manager',
                     'Portal Manager', 'Portal owner', 'ACTIVE', now(), now())
                """);

        jdbc.update("""
                insert into platform_features
                    (version, identifier, code, name, description, service_id, lifecycle_code, settings, created_at, updated_at)
                select
                    0, '22222222-2222-2222-2222-222222222222', 'APPLICATION', 'Application',
                    'Application feature', id, 'ACTIVE', '{}', now(), now()
                  from platform_services
                 where code = 'portal-manager'
                """);

        jdbc.update("""
                insert into messages
                    (version, identifier, service_code, message_key, code, http_status,
                     lifecycle_code, observation, created_at, updated_at)
                values
                    (0, '44444444-4444-4444-4444-444444444444', 'portal-manager',
                     'platform.migration', 'PLATFORM-0001', 400, 'ACTIVE',
                     'Migration reference', now(), now())
                """);

        jdbc.update("""
                insert into platform_feature_scopes (feature_id, feature_scope_code)
                select id, 'MANAGER_ACCOUNT'
                  from platform_features
                 where code = 'APPLICATION'
                """);

        Flyway upgradeFlyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("13")
                .load();
        upgradeFlyway.migrate();

        assertThat(upgradeFlyway.info().current().getVersion().getVersion()).isEqualTo("13");

        assertThat(jdbc.queryForObject(
                "select name from platform_services where code = 'PORTAL_MANAGER'", String.class))
                .isEqualTo("Portal Manager");

        assertThat(jdbc.queryForObject(
                "select service_code from messages where identifier = '44444444-4444-4444-4444-444444444444'",
                String.class
        )).isEqualTo("PORTAL_MANAGER");
        assertThat(jdbc.queryForObject(
                "select name from platform_features where code = 'APPLICATION'", String.class))
                .isEqualTo("Application");

        assertThat(jdbc.queryForObject("""
                select count(*)
                  from platform_feature_context_relations r
                  join platform_features f on f.id = r.feature_id
                  join platform_feature_contexts c on c.id = r.feature_context_id
                 where f.code = 'APPLICATION'
                   and c.code = 'MANAGER_ACCOUNT'
                """, Integer.class)).isEqualTo(1);

        assertThat(jdbc.queryForObject(
                "select lifecycle_code from platform_feature_contexts where code = 'ADMIN_ACCOUNT'", String.class))
                .isEqualTo("INACTIVE");

        assertThat(jdbc.queryForObject(
                "select name from platform_feature_contexts where code = 'MANAGER_ACCOUNT'", String.class))
                .isEqualTo("Manager account");

        assertThat(tableCount(jdbc, "platform_feature_contexts")).isEqualTo(1);
        assertThat(tableCount(jdbc, "platform_feature_context_relations")).isEqualTo(1);
        assertThat(tableCount(jdbc, "type_feature_scopes")).isZero();
        assertThat(tableCount(jdbc, "platform_feature_scopes")).isZero();

        assertThat(jdbc.queryForObject("""
                select service_code
                  from vw_feature_runtime_config
                 where feature_code = 'APPLICATION'
                """, String.class)).isEqualTo("PORTAL_MANAGER");
    }

    private Integer tableCount(JdbcTemplate jdbc, String tableName) {
        return jdbc.queryForObject("""
                select count(*) from information_schema.tables
                 where table_schema = database() and table_name = ?
                """, Integer.class, tableName);
    }
}
