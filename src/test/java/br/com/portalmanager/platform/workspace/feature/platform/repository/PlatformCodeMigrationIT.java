package br.com.portalmanager.platform.workspace.feature.platform.repository;

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
        "spring.flyway.target=14",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlatformCodeMigrationIT {

    @Autowired private DataSource dataSource;
    @Autowired private Flyway flyway;

    @Test
    void shouldMigratePlatformCodesToKebabCaseAndKeepMessageReference() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("14");

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
                    (0, '11111111-1111-1111-1111-111111111111', 'WORKSPACE_SERVICE',
                     'Workspace Service', 'Workspace owner', 'ACTIVE', now(), now())
                """);

        jdbc.update("""
                insert into platform_features
                    (version, identifier, code, name, description, service_id, lifecycle_code, settings, created_at, updated_at)
                select
                    0, '22222222-2222-2222-2222-222222222222', 'PROMOTION_ENGINE',
                    'Promotion Engine', 'Promotion feature', id, 'ACTIVE', '{}', now(), now()
                  from platform_services
                 where code = 'WORKSPACE_SERVICE'
                """);

        jdbc.update("""
                insert into platform_feature_contexts
                    (version, identifier, code, name, description, lifecycle_code, created_at, updated_at)
                values
                    (0, '33333333-3333-3333-3333-333333333333', 'MANAGER_ACCOUNT',
                     'Manager Account', 'Manager context', 'ACTIVE', now(), now())
                """);

        jdbc.update("""
                insert into messages
                    (version, identifier, service_code, message_key, code, http_status,
                     lifecycle_code, observation, created_at, updated_at)
                values
                    (0, '44444444-4444-4444-4444-444444444444', 'WORKSPACE_SERVICE',
                     'platform.code.migration', 'PLATFORM-0001', 400, 'ACTIVE',
                     'Migration reference', now(), now())
                """);

        Flyway upgradeFlyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("15")
                .load();
        upgradeFlyway.migrate();

        assertThat(upgradeFlyway.info().current().getVersion().getVersion()).isEqualTo("15");

        assertThat(jdbc.queryForObject(
                "select code from platform_services where identifier = '11111111-1111-1111-1111-111111111111'",
                String.class
        )).isEqualTo("workspace-service");

        assertThat(jdbc.queryForObject(
                "select code from platform_features where identifier = '22222222-2222-2222-2222-222222222222'",
                String.class
        )).isEqualTo("promotion-engine");

        assertThat(jdbc.queryForObject(
                "select code from platform_feature_contexts where identifier = '33333333-3333-3333-3333-333333333333'",
                String.class
        )).isEqualTo("manager-account");

        assertThat(jdbc.queryForObject(
                "select service_code from messages where identifier = '44444444-4444-4444-4444-444444444444'",
                String.class
        )).isEqualTo("workspace-service");
    }
}
