package br.com.portalmanager.platform.workspace.integration.core.workspace.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

class DatabaseSchemaIT extends br.com.portalmanager.platform.workspace.integration.support.WorkspaceIntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldExposeCurrentSchemaTableSetAndIndexes() {
        assertThat(tableExists("type_features")).isFalse();
        assertThat(tableExists("type_services")).isFalse();
        assertThat(tableExists("type_feature_scopes")).isFalse();
        assertThat(tableExists("type_resource_scopes")).isTrue();
        assertThat(tableExists("type_publisher_scopes")).isFalse();
        assertThat(tableExists("type_schemas")).isFalse();
        assertThat(tableExists("service_context_microservices")).isTrue();
        assertThat(tableExists("platform_microservices")).isFalse();
        assertThat(tableExists("platform_services")).isFalse();
        assertThat(tableExists("type_languages")).isFalse();
        assertThat(tableExists("type_infrastructures")).isFalse();
        assertThat(tableExists("service_context_features")).isTrue();
        assertThat(tableExists("feature_group")).isTrue();
        assertThat(tableExists("feature_group_feature")).isTrue();
        assertThat(tableExists("service_context_groups")).isFalse();
        assertThat(tableExists("service_context_feature_groups")).isFalse();
        assertThat(tableExists("platform_features")).isFalse();
        assertThat(tableExists("platform_feature_contexts")).isFalse();
        assertThat(tableExists("platform_feature_context_relations")).isFalse();
        assertThat(tableExists("platform_feature_scopes")).isFalse();
        assertThat(tableExists("workspaces")).isTrue();
        assertThat(tableExists("workspace_approvers")).isTrue();
        assertThat(tableExists("tags")).isTrue();
        assertThat(indexCount("workspaces", "idx_workspaces_authorizer_group")).isEqualTo(1);
    }

    @Test
    void shouldExposeTheCompleteCurrentTableSetAndConfigurationConstraints() {
        List<String> tables = jdbcTemplate.queryForList(
                """
                select table_name
                  from information_schema.tables
                 where table_schema = database()
                   and table_type = 'BASE TABLE'
                   and table_name <> 'flyway_schema_history'
                """,
                String.class
        );

        assertThat(tables).containsExactlyInAnyOrderElementsOf(Set.of(
                "application_environment_configuration_publishers",
                "application_environment_configurations",
                "application_tag", "applications", "audit_outbox", "category",
                "environment_type_compatibilities", "environment_types", "environments",
                "feature_group", "feature_group_feature", "message_translators", "messages",
                "parameter_keys", "publishers", "resource_sharing_contracts",
                "resource_sharing_environment_mappings", "resource_sharing_participants",
                "schema_binding", "schema_definitions", "schema_versions", "segment",
                "service_context_features", "service_context_microservices", "tags",
                "type_application_scopes", "type_authorizations", "type_life_cycle",
                "type_onboardings", "type_parameter_key_statuses", "type_parameter_key_types",
                "type_publication_modes", "type_resource_scopes", "type_schema_scopes",
                "type_schema_version_status", "type_sharing_statuses", "type_tag_origins",
                "type_visibilities", "type_workspaces", "workspace_approvers",
                "workspace_environment_configuration_publishers",
                "workspace_environment_configurations", "workspace_onboarding_completions",
                "workspace_tag", "workspaces"
        ));

        assertThat(constraintCount("workspace_environment_configurations",
                "uk_workspace_env_config_owner_environment", "UNIQUE")).isEqualTo(1);
        assertThat(constraintCount("application_environment_configurations",
                "uk_application_env_config_owner_environment", "UNIQUE")).isEqualTo(1);
        assertThat(constraintCount("workspace_environment_configuration_publishers",
                "fk_workspace_env_config_publisher_catalog", "FOREIGN KEY")).isEqualTo(1);
        assertThat(constraintCount("application_environment_configuration_publishers",
                "fk_application_env_config_publisher_catalog", "FOREIGN KEY")).isEqualTo(1);
        assertThat(constraintCount("workspace_environment_configuration_publishers",
                "ck_workspace_env_config_order", "CHECK")).isEqualTo(1);
        assertThat(constraintCount("application_environment_configuration_publishers",
                "ck_application_env_config_order", "CHECK")).isEqualTo(1);
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

    private Integer constraintCount(String tableName, String constraintName, String constraintType) {
        return jdbcTemplate.queryForObject(
                """
                select count(*)
                  from information_schema.table_constraints
                 where table_schema = database()
                   and table_name = ?
                   and constraint_name = ?
                   and constraint_type = ?
                """,
                Integer.class,
                tableName,
                constraintName,
                constraintType
        );
    }
}
