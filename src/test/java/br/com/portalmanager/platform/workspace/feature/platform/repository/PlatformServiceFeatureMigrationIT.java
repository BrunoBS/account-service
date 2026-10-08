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
        "spring.flyway.target=11",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PlatformServiceFeatureMigrationIT {

    @Autowired
    private DataSource dataSource;
    @Autowired
    private Flyway flyway;

    @Test
    void shouldMigrateLegacyServiceFeatureOwnershipSettingsScopesAndRuntimeView() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("11");

        // Catalog tables are intentionally empty in a clean V11 schema until the
        // application seeds them. Seed the real V11 lifecycle states required by
        // the legacy rows used by this migration test.
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
                insert into type_services (code, label, description, sort_order, is_active, settings)
                values ('workspace-service', 'Workspace Service', 'Compatibility owner', 1, true, '{}'),
                       ('audit-service', 'Audit Service', 'Audit owner', 2, true, '{}')
                """);
        jdbc.update("""
                insert into type_feature_scopes (code, label, description, sort_order, is_active, settings)
                values ('ADMINISTRATION', 'Administration', 'Administrative scope', 1, true, '{}'),
                       ('CONFIGURATION', 'Configuration', 'Configuration scope', 2, true, '{}')
                """);
        jdbc.update("""
                insert into type_features (code, label, description, sort_order, is_active, settings)
                values (
                    'AUDIT', 'Audit', 'Audit feature', 1, true,
                    '{"service":"audit-service","scopes":["ADMINISTRATION","CONFIGURATION"],"quarantine":{"enabled":true,"retentionDays":35,"restoreAllowed":true},"audit":{"enabled":true,"snapshotOnPurge":true},"purge":{"enabled":true}}'
                ), (
                    'LEGACY', 'Legacy', 'Legacy feature without structural owner', 2, true,
                    '{"quarantine":{"enabled":true,"retentionDays":30,"restoreAllowed":true},"audit":{"enabled":true,"snapshotOnPurge":true},"purge":{"enabled":true}}'
                )
                """);

        Flyway upgradeFlyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .target("12")
                .load();
        upgradeFlyway.migrate();

        assertThat(upgradeFlyway.info().current().getVersion().getVersion()).isEqualTo("12");

        assertThat(jdbc.queryForObject("""
                select s.code
                  from platform_features f
                  join platform_services s on s.id = f.service_id
                 where f.code = 'AUDIT'
                """, String.class)).isEqualTo("audit-service");

        assertThat(jdbc.queryForObject("""
                select s.code
                  from platform_features f
                  join platform_services s on s.id = f.service_id
                 where f.code = 'LEGACY'
                """, String.class)).isEqualTo("workspace-service");

        assertThat(jdbc.queryForObject(
                "select version from platform_services where code = 'audit-service'", Long.class))
                .isZero();
        assertThat(jdbc.queryForObject(
                "select version from platform_features where code = 'AUDIT'", Long.class))
                .isZero();

        String settings = jdbc.queryForObject(
                "select cast(settings as char) from platform_features where code = 'AUDIT'", String.class);
        assertThat(settings).contains("\"retentionDays\": 35");
        assertThat(settings).doesNotContain("\"service\"");
        assertThat(settings).doesNotContain("\"scopes\"");

        assertThat(jdbc.queryForObject("""
                select count(*) from platform_feature_scopes pfs
                join platform_features f on f.id = pfs.feature_id
                where f.code = 'AUDIT'
                """, Integer.class)).isEqualTo(2);

        assertThat(jdbc.queryForObject("""
                select service_code from vw_feature_runtime_config where feature_code = 'AUDIT'
                """, String.class)).isEqualTo("audit-service");
        assertThat(jdbc.queryForObject("""
                select quarantine_retention_days from vw_feature_runtime_config where feature_code = 'AUDIT'
                """, Integer.class)).isEqualTo(35);

        assertThat(tableCount(jdbc, "type_services")).isZero();
        assertThat(tableCount(jdbc, "type_features")).isZero();
    }

    private Integer tableCount(JdbcTemplate jdbc, String tableName) {
        return jdbc.queryForObject("""
                select count(*) from information_schema.tables
                 where table_schema = database() and table_name = ?
                """, Integer.class, tableName);
    }
}
