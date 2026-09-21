package br.com.portalmanager.account.persistence;

import br.com.portalmanager.core.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.core.testing.annotation.WithMySql;
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
    void shouldApplyInitialMigrationOnEmptyMySqlDatabase() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where version = '1' and success = 1",
                Integer.class
        );

        Integer accountTableCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                  from information_schema.tables
                 where table_schema = database()
                   and table_name = 'accounts'
                """,
                Integer.class
        );

        Integer approverTableCount = jdbcTemplate.queryForObject(
                """
                select count(*)
                  from information_schema.tables
                 where table_schema = database()
                   and table_name = 'account_approvers'
                """,
                Integer.class
        );

        assertThat(migrationCount).isEqualTo(1);
        assertThat(accountTableCount).isEqualTo(1);
        assertThat(approverTableCount).isEqualTo(1);
    }
}
