package br.com.portalmanager.platform.workspace.core.workspace.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseSchemaIT extends br.com.portalmanager.platform.workspace.support.WorkspaceIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldExposeTheCurrentSchemaAndRequiredReferenceData() {
        assertThat(tableCount("type_features")).isZero();
        assertThat(tableCount("type_services")).isZero();
        assertThat(tableCount("type_feature_scopes")).isZero();
        assertThat(tableCount("type_resource_scopes")).isEqualTo(1);
        assertThat(tableCount("type_publisher_scopes")).isZero();
        assertThat(tableCount("type_schemas")).isZero();
        assertThat(tableCount("service_context_microservices")).isEqualTo(1);
        assertThat(tableCount("platform_microservices")).isZero();
        assertThat(tableCount("platform_services")).isZero();
        assertThat(tableCount("type_languages")).isZero();
        assertThat(tableCount("type_infrastructures")).isZero();
        assertThat(tableCount("service_context_features")).isEqualTo(1);
        assertThat(tableCount("feature_group")).isEqualTo(1);
        assertThat(tableCount("feature_group_feature")).isEqualTo(1);
        assertThat(tableCount("service_context_groups")).isZero();
        assertThat(tableCount("service_context_feature_groups")).isZero();
        assertThat(tableCount("platform_features")).isZero();
        assertThat(tableCount("platform_feature_contexts")).isZero();
        assertThat(tableCount("platform_feature_context_relations")).isZero();
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
