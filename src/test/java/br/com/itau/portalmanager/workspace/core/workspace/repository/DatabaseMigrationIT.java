package br.com.itau.portalmanager.workspace.core.workspace.repository;

import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@PlatformIntegrationTest
@WithMySql
class DatabaseMigrationIT {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyGoldenMigrationsAndExposeWorkspaceSchema() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version in ('1', '2', '3', '4') and success = 1",
                Integer.class
        );

        Integer workspaceTableCount = tableCount("workspaces");
        Integer approverTableCount = tableCount("workspace_approvers");
        Integer tagsTableCount = tableCount("tags");
        Integer legacyWorkspaceTableCount = tableCount("accounts");
        Integer legacyApproverTableCount = tableCount("account_approvers");

        assertThat(migrationCount).isEqualTo(4);
        assertThat(tableCount("type_workspaces")).isEqualTo(1);
        assertThat(tableCount("type_features")).isEqualTo(1);
        assertThat(tableCount("type_schemas")).isEqualTo(1);
        assertThat(tableCount("type_onboardings")).isEqualTo(1);
        assertThat(workspaceTableCount).isEqualTo(1);
        assertThat(approverTableCount).isEqualTo(1);
        assertThat(tagsTableCount).isEqualTo(1);
        assertThat(legacyWorkspaceTableCount).isZero();
        assertThat(legacyApproverTableCount).isZero();
    }

    private Integer tableCount(String tableName) {
        return jdbcTemplate.queryForObject(
                """
                select count(*)
                  from information_schema.tables
                 where table_schema = database()
                   and table_name = ?
                """,
                Integer.class,
                tableName
        );
    }
}
