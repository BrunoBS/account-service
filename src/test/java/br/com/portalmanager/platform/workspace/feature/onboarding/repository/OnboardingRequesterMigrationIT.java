package br.com.portalmanager.platform.workspace.feature.onboarding.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.portalmanager.platform.library.testing.database.annotation.WithMySql;
import br.com.portalmanager.platform.library.testing.lifecycle.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.workspace.support.MigrationSchemaValidationTestConfiguration;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@PlatformIntegrationTest
@WithMySql
@Import(MigrationSchemaValidationTestConfiguration.class)
@TestPropertySource(properties = {
    "spring.flyway.target=59", "platform.schema-validation.enabled=false", "spring.jpa.hibernate.ddl-auto=none"
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class OnboardingRequesterMigrationIT {

    @Autowired
    private DataSource dataSource;

    @Test
    void refusesToTruncateLegacyRequestersAndPreservesHistoryAfterUpgrade() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("INSERT IGNORE INTO type_workspaces (code,label,sort_order,is_active,settings) VALUES ('MANAGER','Manager',1,true,'{}')");
        jdbc.update("INSERT IGNORE INTO type_life_cycle (code,label,sort_order,is_active,settings) VALUES ('ACTIVE','Active',1,true,'{}')");
        jdbc.update("INSERT IGNORE INTO type_onboardings (code,label,sort_order,is_active,settings) VALUES ('WORKSPACE_REGISTRATION','Registration',1,true,'{}')");
        jdbc.update("""
            INSERT INTO workspaces (version, identifier, workspace_type_code, name, description, requester,
                acronym, settings, email_group, onboarding, lifecycle_code, created_at, updated_at)
            VALUES (0, UUID(), 'MANAGER', 'Requester migration', 'Migration test', 'requester',
                'ONB', '{}', 'workspace@example.com', false, 'ACTIVE', NOW(6), NOW(6))
            """);
        jdbc.update("""
            INSERT INTO workspace_onboarding_completions
                (workspace_id, onboarding_phase_code, user_identifier, completed_at)
            SELECT id, 'WORKSPACE_REGISTRATION', '12345678901', '2026-10-01 12:00:00'
            FROM workspaces
            """);
        Flyway upgrade = Flyway.configure().dataSource(dataSource)
            .locations("classpath:db/migration").target("60").load();

        assertThatThrownBy(upgrade::migrate).isInstanceOf(FlywayException.class);
        assertThat(jdbc.queryForObject("SELECT user_identifier FROM workspace_onboarding_completions", String.class))
            .isEqualTo("12345678901");

        // Simulate an explicit correction of legacy identity before retrying the migration.
        jdbc.update("UPDATE workspace_onboarding_completions SET user_identifier = '1234567890'");
        List<Map<String, Object>> originalHistory = jdbc.queryForList(
            "SELECT id, workspace_id, onboarding_phase_code, completed_at FROM workspace_onboarding_completions"
        );
        upgrade.repair();
        upgrade.migrate();

        assertThat(upgrade.info().current().getVersion().getVersion()).isEqualTo("60");
        assertThat(jdbc.queryForObject("SELECT requester FROM workspace_onboarding_completions", String.class))
            .isEqualTo("1234567890");
        assertThat(jdbc.queryForList(
            "SELECT id, workspace_id, onboarding_phase_code, completed_at FROM workspace_onboarding_completions"
        )).isEqualTo(originalHistory);
        assertThat(jdbc.queryForObject("""
            SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = database() AND TABLE_NAME = 'workspace_onboarding_completions'
              AND COLUMN_NAME = 'requester'
            """, Long.class)).isEqualTo(10L);
    }
}
