package br.com.portalmanager.platform.workspace.core.workspace.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseSchemaIT extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldExposeCurrentTablesAndIndexes() {
        assertThat(tableExists("type_features")).isTrue();
        assertThat(tableExists("type_services")).isTrue();
        assertThat(tableExists("type_feature_scopes")).isTrue();
        assertThat(tableExists("type_resource_scopes")).isTrue();
        assertThat(tableExists("type_publisher_scopes")).isTrue();
        assertThat(tableExists("type_schemas")).isTrue();
        assertThat(tableExists("service_context_microservices")).isTrue();
        assertThat(tableExists("platform_microservices")).isTrue();
        assertThat(tableExists("platform_services")).isTrue();
        assertThat(tableExists("type_languages")).isTrue();
        assertThat(tableExists("type_infrastructures")).isTrue();
        assertThat(tableExists("service_context_features")).isTrue();
        assertThat(tableExists("feature_group")).isTrue();
        assertThat(tableExists("feature_group_feature")).isTrue();
        assertThat(tableExists("service_context_groups")).isTrue();
        assertThat(tableExists("service_context_feature_groups")).isTrue();
        assertThat(tableExists("platform_features")).isTrue();
        assertThat(tableExists("platform_feature_contexts")).isTrue();
        assertThat(tableExists("platform_feature_context_relations")).isTrue();
        assertThat(tableExists("platform_feature_scopes")).isTrue();
        assertThat(tableExists("workspaces")).isTrue();
        assertThat(tableExists("workspace_approvers")).isTrue();
        assertThat(tableExists("tags")).isTrue();
        assertThat(indexCount("workspaces", "idx_workspaces_authorizer_group")).isEqualTo(1);
    }

    private boolean tableExists(String tableName) {
        return jdbcTemplate.queryForObject(
            """
            select count(*)
              from information_schema.tables
             where table_schema = database()
               and table_name = ?
            """,
            Integer.class,
            tableName
        ) == 1;
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
