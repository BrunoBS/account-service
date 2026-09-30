package br.com.portalmanager.platform.workspace.core.workspace.repository;

import br.com.portalmanager.platform.library.testing.annotation.PlatformIntegrationTest;
import br.com.portalmanager.platform.library.testing.annotation.WithMySql;
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
        assertThat(tableCount("type_features")).isZero();
        assertThat(tableCount("type_services")).isZero();
        assertThat(tableCount("type_feature_scopes")).isZero();
        assertThat(tableCount("type_resource_scopes")).isEqualTo(1);
        assertThat(tableCount("type_publisher_scopes")).isZero();
        assertThat(tableCount("type_schemas")).isZero();
        assertThat(tableCount("platform_microservices")).isEqualTo(1);
        assertThat(tableCount("platform_services")).isZero();
        assertThat(tableCount("type_languages")).isZero();
        assertThat(tableCount("type_infrastructures")).isZero();
        assertThat(tableCount("platform_features")).isEqualTo(1);
        assertThat(tableCount("platform_feature_contexts")).isEqualTo(1);
        assertThat(tableCount("platform_feature_context_relations")).isEqualTo(1);
        assertThat(tableCount("platform_feature_scopes")).isZero();
        assertThat(tableCount("workspaces")).isEqualTo(1);
        assertThat(tableCount("workspace_approvers")).isEqualTo(1);
        assertThat(tableCount("tags")).isEqualTo(1);
        assertThat(indexCount("workspaces", "idx_workspaces_authorizer_group")).isEqualTo(1);
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

    private Integer indexCount(String tableName, String indexName) {
        return jdbcTemplate.queryForObject(
                """
                        select count(*)
                          from information_schema.statistics
                         where table_schema = database()
                           and table_name = ?
                           and index_name = ?
                        """,
                Integer.class,
                tableName,
                indexName
        );
    }
}
