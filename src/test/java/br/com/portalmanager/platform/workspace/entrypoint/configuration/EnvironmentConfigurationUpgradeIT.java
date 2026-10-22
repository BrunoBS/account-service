package br.com.portalmanager.platform.workspace.entrypoint.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(
        properties = {
            "spring.flyway.target=83",
            "platform.schema-validation.enabled=false",
            "spring.jpa.hibernate.ddl-auto=none",
            "platform.authorization.mode=MOCK",
            "platform.audit.enabled=false"
        })
class EnvironmentConfigurationUpgradeIT {
    @Autowired DataSource dataSource;
    @Autowired Flyway flyway;

    @Test
    void upgradeFromV83PreservesExistingWorkspaceSettingsAndVersion() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("83");
        jdbc.update(
                "insert ignore into type_life_cycle(code,label,sort_order,is_active,settings)"
                        + " values ('ACTIVE','Active',1,true,'{}')");
        jdbc.update(
                "insert ignore into type_workspaces(code,label,sort_order,is_active,settings)"
                        + " values ('MANAGER','Manager',1,true,'{}')");
        String identifier = "2a55e118-a599-42f0-8528-06d11b2e287b";
        jdbc.update(
                """
                insert into workspaces(version,identifier,workspace_type_code,name,description,requester,acronym,settings,email_group,onboarding,lifecycle_code,created_at,updated_at)
                values (7,?,'MANAGER','Preserved owner','Preserved description','requester','PRES','{"registered":true,"limit":123}','test@example.com',false,'ACTIVE',now(6),now(6))
                """,
                identifier);
        Flyway upgraded =
                Flyway.configure()
                        .dataSource(dataSource)
                        .locations("classpath:db/migration")
                        .load();
        upgraded.migrate();
        upgraded.validate();
        assertThat(upgraded.info().current().getVersion().getVersion()).isEqualTo("85");
        assertThat(
                        jdbc.queryForObject(
                                "select version from workspaces where identifier=?",
                                Long.class,
                                identifier))
                .isEqualTo(7L);
        assertThat(
                        jdbc.queryForObject(
                                "select json_extract(settings,'$.limit') from workspaces where"
                                        + " identifier=?",
                                String.class,
                                identifier))
                .isEqualTo("123");
        for (String table :
                java.util.List.of(
                        "workspace_environment_configurations",
                        "application_environment_configurations",
                        "workspace_environment_configuration_publishers",
                        "application_environment_configuration_publishers")) {
            assertThat(jdbc.queryForObject("select count(*) from " + table, Long.class)).isZero();
        }
    }
}
