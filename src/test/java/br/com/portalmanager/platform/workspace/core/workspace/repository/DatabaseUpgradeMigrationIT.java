package br.com.portalmanager.platform.workspace.core.workspace.repository;

import br.com.portalmanager.platform.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.testing.annotation.WithMySql;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@PlatformIntegrationTest
@WithMySql
@TestPropertySource(properties = {
        "spring.flyway.target=2",
        "spring.jpa.hibernate.ddl-auto=none"
})
class DatabaseUpgradeMigrationIT {

    private static final String IDENTIFIER = "11111111-2222-3333-4444-555555555555";

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Test
    void shouldUpgradePopulatedV2SchemaToLatestWithoutLosingWorkspaceData() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");

        jdbc.update(
                """
                insert into accounts (
                    id, version, identifier, account_type, name, description, requester,
                    acronym, settings, authorizer_group, email_group, onboarding, lifecycle,
                    created_at, updated_at
                ) values (
                    100, 3, ?, 'ADMIN', 'Legacy Workspace', 'Descrição persistida antes da V3',
                    'legacy-requester', 'LEG', '{"legacy":true}', 'TEAM_LEGACY',
                    'legacy@portalmanager.com', true, 'INACTIVE', now(6), now(6)
                )
                """,
                IDENTIFIER
        );

        jdbc.update(
                """
                insert into account_approvers (id, account_id, functional, email)
                values ('approver-before-v3', 100, 'F12345', 'approver@portalmanager.com')
                """
        );

        jdbc.update(
                """
                insert into tags (id, owner_type, owner_id, name, origin_type)
                values ('tag-before-v3', 'ACCOUNT', ?, 'legacy-tag', 'MANUAL')
                """,
                IDENTIFIER
        );

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .load()
                .migrate();

        assertThat(tableCount(jdbc, "accounts")).isZero();
        assertThat(tableCount(jdbc, "account_approvers")).isZero();
        assertThat(tableCount(jdbc, "workspaces")).isEqualTo(1);
        assertThat(tableCount(jdbc, "workspace_approvers")).isEqualTo(1);
        assertThat(tableCount(jdbc, "type_workspaces")).isEqualTo(1);
        assertThat(tableCount(jdbc, "type_schema_scopes")).isEqualTo(1);
        assertThat(tableCount(jdbc, "type_life_cycle")).isEqualTo(1);

        assertThat(jdbc.queryForObject(
                "select workspace_type_code from workspaces where id = 100",
                String.class
        )).isEqualTo("ADMIN");

        assertThat(jdbc.queryForObject(
                "select version from workspaces where id = 100",
                Long.class
        )).isEqualTo(3L);

        assertThat(jdbc.queryForObject(
                "select lifecycle_code from workspaces where id = 100",
                String.class
        )).isEqualTo("INACTIVE");

        assertThat(jdbc.queryForObject(
                "select workspace_id from workspace_approvers where id = 'approver-before-v3'",
                Long.class
        )).isEqualTo(100L);

        assertThat(jdbc.queryForObject(
                "select owner_type from tags where id = 'tag-before-v3'",
                String.class
        )).isEqualTo("WORKSPACE");

        assertThat(jdbc.queryForObject(
                "select owner_id from tags where id = 'tag-before-v3'",
                String.class
        )).isEqualTo(IDENTIFIER);

        jdbc.update("delete from workspaces where id = 100");

        assertThat(jdbc.queryForObject(
                "select count(*) from workspace_approvers where workspace_id = 100",
                Integer.class
        )).isZero();
    }

    private Integer tableCount(JdbcTemplate jdbc, String tableName) {
        return jdbc.queryForObject(
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
